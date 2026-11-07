#!/usr/bin/env python3
"""
Downloads pre-built MorphoDiTa binaries from GitHub Releases (only if missing).

Usage:
    python fetch_natives.py
    MORPHODITA_VERSION=1.12.0 python fetch_natives.py

Environment:
    MORPHODITA_VERSION - version to download (default: 1.11.3)
"""

import json
import os
import shutil
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path
from typing import Optional

DEFAULT_VERSION = "1.11.3"
GITHUB_RELEASES_URL = "https://github.com/ufal/morphodita/releases/download"
GITHUB_API_URL = "https://api.github.com/repos/ufal/morphodita/releases/latest"

# Native library names per platform
NATIVE_LIBS = {
    "linux-x86_64": "libmorphodita_java.so",
    "windows-x86_64": "morphodita_java.dll"
}


def get_script_dir() -> Path:
    """Get directory where this script is located."""
    return Path(__file__).parent.resolve()


def get_output_dir() -> Path:
    """Get output directory for native libraries."""
    return get_script_dir().parent / "src" / "main" / "resources" / "native"


def print_header(version: str):
    """Print script header."""
    print("=== MorphoDiTa Native Library Fetcher ===")
    print(f"Version: {version}")
    print()


def check_existing_libraries(output_dir: Path) -> dict:
    """Check which libraries already exist locally."""
    status = {}
    for platform, lib_name in NATIVE_LIBS.items():
        lib_path = output_dir / platform / lib_name
        if lib_path.is_file():
            print(f"[INFO] {platform} library already present: {lib_path}")
            print("[INFO] Skipping download. Delete the file to force re-download.")
            status[platform] = True
        else:
            status[platform] = False
    return status


def download_file(url: str, dest: Path) -> bool:
    """Download file from URL to destination."""
    try:
        print(f"[INFO] Downloading from {url}...")
        req = urllib.request.Request(url, headers={"User-Agent": "MorphoDiTa-Fetch"})
        with urllib.request.urlopen(req, timeout=60) as response:
            with open(dest, 'wb') as f:
                shutil.copyfileobj(response, f)
        return True
    except Exception as e:
        print(f"[ERROR] Download failed: {e}")
        return False


def extract_and_copy_libraries(
    archive_path: Path,
    output_dir: Path,
    status: dict
) -> dict:
    """Extract archive and copy native libraries to output directory."""
    results = {}

    with tempfile.TemporaryDirectory() as temp_dir:
        extract_dir = Path(temp_dir) / "extracted"

        print("[INFO] Extracting archive...")
        with zipfile.ZipFile(archive_path, 'r') as zf:
            zf.extractall(extract_dir)

        # Find and copy libraries
        for platform, lib_name in NATIVE_LIBS.items():
            if status.get(platform, False):
                results[platform] = True
                continue

            # Search for library in extracted files
            lib_files = list(extract_dir.rglob(lib_name))
            if lib_files:
                src = lib_files[0]
                dest_dir = output_dir / platform
                dest_dir.mkdir(parents=True, exist_ok=True)
                dest = dest_dir / lib_name
                shutil.copy2(src, dest)
                print(f"[OK] {platform} library installed: {dest}")
                results[platform] = True
            else:
                print(f"[WARN] {platform} library ({lib_name}) not found in archive")
                print("[INFO] You may need to download platform-specific binaries separately")
                results[platform] = False

    return results


def fetch_latest_version() -> Optional[str]:
    """Fetch latest version from GitHub API."""
    try:
        req = urllib.request.Request(
            GITHUB_API_URL,
            headers={
                "User-Agent": "MorphoDiTa-Fetch",
                "Accept": "application/vnd.github.v3+json"
            }
        )
        with urllib.request.urlopen(req, timeout=10) as response:
            data = json.loads(response.read().decode())
            tag = data.get("tag_name", "")
            return tag.lstrip("v") if tag else None
    except Exception:
        return None


def check_for_updates(current_version: str, output_dir: Path):
    """Check for newer versions on GitHub."""
    print()
    print("[INFO] Checking for updates...")

    latest = fetch_latest_version()
    if not latest:
        print("[WARN] Could not check for updates")
        return

    if latest != current_version and is_newer(latest, current_version):
        print()
        print("╔══════════════════════════════════════════════════════════════════════╗")
        print("║  [UPDATE AVAILABLE]                                                  ║")
        print("║                                                                      ║")
        print(f"║  Current version: v{current_version:<52}║")
        print(f"║  Latest version:  v{latest:<52}║")
        print("║                                                                      ║")
        print("║  To update manually:                                                 ║")
        print(f"║  rm -rf {str(output_dir):<60}║")
        print(f"║  MORPHODITA_VERSION={latest} python fetch_natives.py                ║")
        print("╚══════════════════════════════════════════════════════════════════════╝")
    else:
        print(f"[OK] You have the latest version (v{current_version})")


def is_newer(version1: str, version2: str) -> bool:
    """Compare two semantic version strings."""
    def parse_parts(v: str) -> list:
        parts = []
        for p in v.split("."):
            # Remove non-numeric suffix
            numeric = ""
            for c in p:
                if c.isdigit():
                    numeric += c
                else:
                    break
            parts.append(int(numeric) if numeric else 0)
        return parts

    parts1 = parse_parts(version1)
    parts2 = parse_parts(version2)
    max_len = max(len(parts1), len(parts2))

    for i in range(max_len):
        v1 = parts1[i] if i < len(parts1) else 0
        v2 = parts2[i] if i < len(parts2) else 0
        if v1 > v2:
            return True
        elif v1 < v2:
            return False
    return False


def main():
    """Main entry point."""
    version = os.environ.get("MORPHODITA_VERSION", DEFAULT_VERSION)
    output_dir = get_output_dir()

    print_header(version)

    # Check existing libraries
    status = check_existing_libraries(output_dir)

    # Download if any library is missing
    need_download = not all(status.values())

    if need_download:
        print()
        print("[INFO] Downloading pre-built binaries from GitHub...")

        # Ensure output directories exist
        for platform in NATIVE_LIBS:
            (output_dir / platform).mkdir(parents=True, exist_ok=True)

        # Download archive
        archive_name = f"morphodita-{version}-bin.zip"
        archive_url = f"{GITHUB_RELEASES_URL}/v{version}/{archive_name}"

        with tempfile.TemporaryDirectory() as temp_dir:
            archive_path = Path(temp_dir) / archive_name

            if not download_file(archive_url, archive_path):
                print("[ERROR] Failed to download archive")
                sys.exit(1)

            # Extract and copy libraries
            extract_and_copy_libraries(archive_path, output_dir, status)

    # Check for updates
    check_for_updates(version, output_dir)

    print()
    print("=== Done ===")


if __name__ == "__main__":
    main()
