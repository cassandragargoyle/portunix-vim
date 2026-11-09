#!/usr/bin/env python3
#
# This file is part of CassandraGargoyle Community Project
# Licensed under the MIT License - see LICENSE file for details
#
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
"""Build and test the repository in a clean container, isolated from the host environment.

The container is created with ``portunix container``; Java, Maven and Make are installed with
``portunix install`` inside it (Java falls back to the distribution JDK when the Portunix download
fails). The sources are exported from git (the working tree with uncommitted changes of tracked
files, or any ``--ref``), copied into the container and built there.

GitHub Packages needs credentials even for public packages: ``GITHUB_ACTOR``/``GITHUB_TOKEN`` from
the environment, otherwise ``gh api user`` and ``gh auth token``. The token is copied into the
container as a file readable only by root and never appears on a command line.

Examples::

    uv run scripts/container-build.py
    uv run scripts/container-build.py --ref feature/003-ci-table-detector-artifacts
    uv run scripts/container-build.py --keep --command "make verify"
    uv run scripts/container-build.py --container vim-build-1234 --command "make build"
"""

from __future__ import annotations

import argparse
import os
import shlex
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

VERSION = "0.1.0"

REPO_ROOT = Path(__file__).resolve().parent.parent
WORKDIR = "/work"
ENV_FILE = "/root/.build-env"

# Same command as the vim-parser CI workflow (TK stemmer tests need a native library)
DEFAULT_COMMAND = (
    "mvn -B -ntp -f java/vim-parser/pom.xml clean verify"
    " -Dtest='!org.cassandragargoyle.tk.**,!**.TKBackendTest'"
    " -Dsurefire.failIfNoSpecifiedTests=false"
)

# Portunix helper binaries needed inside the container
PORTUNIX_BINARIES = ("portunix", "ptx-installer")


class BuildError(RuntimeError):
    """Failure of a preparation step"""


def log(message: str) -> None:
    print(f"==> {message}", flush=True)


def run(cmd: list[str], *, capture: bool = False, check: bool = True,
        stdin: bytes | None = None) -> subprocess.CompletedProcess:
    result = subprocess.run(
        cmd,
        input=stdin,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
        check=False,
    )
    if check and result.returncode != 0:
        output = result.stdout.decode(errors="replace") if capture and result.stdout else ""
        raise BuildError(f"command failed ({result.returncode}): {shlex.join(cmd)}\n{output}")
    return result


def container_exec(name: str, script: str, *, check: bool = True,
                   capture: bool = False) -> subprocess.CompletedProcess:
    # Login shell so PATH changes written by portunix install into ~/.profile apply
    return run(["portunix", "container", "exec", name, "--", "bash", "-lc", script],
               check=check, capture=capture)


def container_cp(source: str, destination: str) -> None:
    run(["portunix", "container", "cp", source, destination], capture=True)


def find_portunix() -> Path:
    portunix = shutil.which("portunix")
    if not portunix:
        raise BuildError("portunix not found in PATH")
    directory = Path(portunix).resolve().parent
    for binary in PORTUNIX_BINARIES:
        if not (directory / binary).is_file():
            raise BuildError(f"{binary} not found next to portunix in {directory}")
    return directory


def github_credentials() -> tuple[str, str]:
    actor = os.environ.get("GITHUB_ACTOR", "")
    token = os.environ.get("GITHUB_TOKEN", "")
    if shutil.which("gh"):
        if not actor:
            actor = run(["gh", "api", "user", "--jq", ".login"], capture=True,
                        check=False).stdout.decode().strip()
        if not token:
            token = run(["gh", "auth", "token"], capture=True,
                        check=False).stdout.decode().strip()
    return actor, token


def export_sources(ref: str | None) -> tuple[bytes, str]:
    """Return a tar archive of the sources and a description of what was exported"""
    git = ["git", "-C", str(REPO_ROOT)]
    if ref:
        tree = ref
        description = ref
    else:
        stash = run(git + ["stash", "create"], capture=True).stdout.decode().strip()
        tree = stash or "HEAD"
        description = "working tree (tracked files incl. uncommitted changes)" if stash else "HEAD"
    commit = run(git + ["rev-parse", "--short", tree], capture=True).stdout.decode().strip()
    archive = run(git + ["archive", "--format=tar", tree], capture=True).stdout
    return archive, f"{description} @ {commit}"


def create_container(name: str, image: str, ttl: str) -> None:
    log(f"Creating container {name} from {image}")
    run(["portunix", "container", "run", "-d", "--name", name, "--ttl", ttl, image,
         "sleep", "infinity"], capture=True)


def install_toolchain(name: str, portunix_dir: Path, java_variant: str) -> None:
    log("Installing CA certificates (needed for HTTPS downloads)")
    container_exec(name, "export DEBIAN_FRONTEND=noninteractive; "
                         "apt-get update -qq && apt-get install -y -qq ca-certificates >/dev/null",
                   capture=True)

    log("Copying portunix into the container")
    for binary in PORTUNIX_BINARIES:
        container_cp(str(portunix_dir / binary), f"{name}:/usr/local/bin/{binary}")

    log(f"Installing Java {java_variant} (portunix install java)")
    result = container_exec(name, f"portunix install java --variant={java_variant}",
                            check=False, capture=True)
    if result.returncode != 0:
        reason = [line for line in result.stdout.decode(errors="replace").splitlines()
                  if "failed" in line.lower()]
        log("WARNING: portunix install java failed: "
            + (reason[0].strip() if reason else f"exit {result.returncode}"))
        log(f"Falling back to the distribution package openjdk-{java_variant}-jdk-headless")
        container_exec(name, "export DEBIAN_FRONTEND=noninteractive; "
                             f"apt-get install -y -qq openjdk-{java_variant}-jdk-headless >/dev/null",
                       capture=True)

    for package in ("maven", "make"):
        log(f"Installing {package} (portunix install {package})")
        container_exec(name, f"portunix install {package}", capture=True)


def write_credentials(name: str, actor: str, token: str, server_ids: list[str]) -> None:
    log(f"Writing Maven settings.xml (servers: {', '.join(server_ids)})")
    with tempfile.NamedTemporaryFile("w", delete=False, prefix="build-env-") as handle:
        os.chmod(handle.name, 0o600)
        handle.write(f"export GITHUB_ACTOR={shlex.quote(actor)}\n")
        handle.write(f"export GITHUB_TOKEN={shlex.quote(token)}\n")
    try:
        container_cp(handle.name, f"{name}:{ENV_FILE}")
    finally:
        os.unlink(handle.name)

    servers = "".join(
        f"<server><id>{server_id}</id><username>${{env.GITHUB_ACTOR}}</username>"
        f"<password>${{env.GITHUB_TOKEN}}</password></server>"
        for server_id in server_ids
    )
    settings = f"<settings><servers>{servers}</servers></settings>"
    container_exec(name, f"chmod 600 {ENV_FILE} && mkdir -p ~/.m2 && "
                         f"printf '%s\\n' {shlex.quote(settings)} > ~/.m2/settings.xml")


def copy_sources(name: str, archive: bytes) -> None:
    with tempfile.NamedTemporaryFile(delete=False, suffix=".tar") as handle:
        handle.write(archive)
    try:
        container_exec(name, f"rm -rf {WORKDIR} && mkdir -p {WORKDIR}")
        container_cp(handle.name, f"{name}:/tmp/sources.tar")
    finally:
        os.unlink(handle.name)
    container_exec(name, f"tar -xf /tmp/sources.tar -C {WORKDIR} && rm /tmp/sources.tar")


def build(name: str, command: str) -> int:
    log("Toolchain")
    container_exec(name, "java -version 2>&1 | head -1; javac -version; "
                         "mvn -v 2>&1 | head -1; make --version | head -1", check=False)
    log(f"Running: {command}")
    script = (
        f"set -a; . {ENV_FILE}; set +a; "
        'export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"; '
        f"cd {WORKDIR} && {command}"
    )
    started = time.monotonic()
    result = container_exec(name, script, check=False)
    log(f"Build finished with exit code {result.returncode} "
        f"in {time.monotonic() - started:.0f} s")
    return result.returncode


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="container-build",
        description="Build and test the repository in a clean Portunix container.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="Default command (same as CI):\n  " + DEFAULT_COMMAND,
    )
    parser.add_argument("-r", "--ref",
                        help="git ref to build (default: working tree incl. uncommitted changes "
                             "of tracked files)")
    parser.add_argument("-c", "--command", default=DEFAULT_COMMAND,
                        help="command to run in the repository root inside the container")
    parser.add_argument("-i", "--image", default="ubuntu:24.04",
                        help="container image (default: %(default)s)")
    parser.add_argument("-j", "--java", default="21",
                        help="Java variant for portunix install (default: %(default)s)")
    parser.add_argument("-s", "--server-id", action="append", dest="server_ids",
                        help="Maven server id for GitHub Packages, repeatable (default: github)")
    parser.add_argument("-n", "--name", help="name of the new container")
    parser.add_argument("--container",
                        help="reuse an existing prepared container (skips the toolchain install)")
    parser.add_argument("-k", "--keep", action="store_true",
                        help="keep the container after the build")
    parser.add_argument("--ttl", default="2h",
                        help="auto-remove the container after this time (default: %(default)s)")
    parser.add_argument("-V", "--version", action="version", version=f"%(prog)s {VERSION}")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(sys.argv[1:] if argv is None else argv)
    server_ids = args.server_ids or ["github"]
    reuse = args.container is not None
    name = args.container or args.name or f"vim-build-{int(time.time())}"

    try:
        archive, description = export_sources(args.ref)
        log(f"Sources: {description}")
        actor, token = github_credentials()
        if not token:
            log("WARNING: no GitHub token (GITHUB_TOKEN or gh auth); "
                "GitHub Packages downloads will fail with 401")

        if not reuse:
            portunix_dir = find_portunix()
            create_container(name, args.image, args.ttl)
        try:
            if not reuse:
                install_toolchain(name, portunix_dir, args.java)
            write_credentials(name, actor, token, server_ids)
            log("Copying sources into the container")
            copy_sources(name, archive)
            return build(name, args.command)
        finally:
            if args.keep or reuse:
                log(f"Container kept: {name} "
                    f"(reuse with --container {name}, remove with "
                    f"'portunix container rm -f {name}')")
            else:
                log(f"Removing container {name}")
                run(["portunix", "container", "rm", "-f", name], capture=True, check=False)
    except BuildError as error:
        print(f"error: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
