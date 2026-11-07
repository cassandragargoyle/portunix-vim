#!/usr/bin/env python3
#
# This file is part of CassandraGargoyle Community Project
# Licensed under the MIT License - see LICENSE file for details
#
"""
Build TK Stemmer JNI native library

Prerequisites:
  - JDK 21+
  - CMake 3.20+
  - GCC 11+ (Linux) or Visual Studio 2022 (Windows)
  - TK Stemmer C++ library

Usage:
  python build_native.py /path/to/tk-stemmer
  python build_native.py --help
"""

import argparse
import os
import platform
import shutil
import subprocess
import sys
from pathlib import Path


def get_platform_info():
    """Detect current platform and return (platform_name, lib_extension)"""
    system = platform.system().lower()
    machine = platform.machine().lower()

    if system == "linux" and machine in ("x86_64", "amd64"):
        return "linux-x86_64", ".so"
    elif system == "windows" and machine in ("x86_64", "amd64"):
        return "windows-x86_64", ".dll"
    elif system == "darwin" and machine == "arm64":
        return "macos-aarch64", ".dylib"
    else:
        raise RuntimeError(f"Unsupported platform: {system}/{machine}")


def find_java_home():
    """Find JAVA_HOME from environment or try to detect it"""
    java_home = os.environ.get("JAVA_HOME")
    if java_home and Path(java_home).exists():
        return Path(java_home)

    # Try common locations
    if platform.system() == "Linux":
        for path in Path("/usr/lib/jvm").glob("java-21-*"):
            if path.is_dir():
                return path

    raise RuntimeError(
        "JAVA_HOME not set. Please set JAVA_HOME environment variable to JDK 21+ location"
    )


def verify_tk_stemmer(tk_dir: Path):
    """Verify TK Stemmer installation"""
    header = tk_dir / "include" / "tk" / "stemmer" / "Stemmer.h"
    if not header.exists():
        raise RuntimeError(
            f"TK Stemmer not found at {tk_dir}\n"
            f"Expected header: {header}"
        )

    # Check for library
    lib_dir = tk_dir / "lib"
    if not lib_dir.exists():
        raise RuntimeError(f"TK Stemmer lib directory not found: {lib_dir}")

    return True


def run_cmake_configure(build_dir: Path, native_dir: Path, java_home: Path, tk_dir: Path):
    """Run CMake configure step"""
    build_dir.mkdir(parents=True, exist_ok=True)

    cmake_args = [
        "cmake",
        f"-DJAVA_HOME={java_home}",
        f"-DTK_STEMMER_DIR={tk_dir}",
        "-DCMAKE_BUILD_TYPE=Release",
    ]

    # Add generator for Windows
    if platform.system() == "Windows":
        cmake_args.extend(["-G", "Visual Studio 17 2022", "-A", "x64"])

    cmake_args.append(str(native_dir))

    print(f"Running: {' '.join(cmake_args)}")
    subprocess.run(cmake_args, cwd=build_dir, check=True)


def run_cmake_build(build_dir: Path):
    """Run CMake build step"""
    cmake_args = ["cmake", "--build", ".", "--config", "Release"]

    if platform.system() != "Windows":
        # Use parallel build on non-Windows
        cpu_count = os.cpu_count() or 1
        cmake_args.extend(["--", f"-j{cpu_count}"])

    print(f"Running: {' '.join(cmake_args)}")
    subprocess.run(cmake_args, cwd=build_dir, check=True)


def copy_libraries(build_dir: Path, resources_dir: Path, tk_dir: Path, platform_name: str):
    """Copy built libraries to resources directory"""
    resources_dir.mkdir(parents=True, exist_ok=True)

    system = platform.system().lower()

    if system == "windows":
        # Windows: DLLs are in Release subdirectory
        jni_lib = build_dir / "Release" / "tk-stemmer-jni.dll"
        base_lib = tk_dir / "bin" / "libtk-stemmer.dll"
    elif system == "darwin":
        jni_lib = build_dir / "libtk-stemmer-jni.dylib"
        base_lib = tk_dir / "lib" / "libtk-stemmer.dylib"
    else:
        jni_lib = build_dir / "libtk-stemmer-jni.so"
        base_lib = tk_dir / "lib" / "libtk-stemmer.so"

    if not jni_lib.exists():
        raise RuntimeError(f"Built JNI library not found: {jni_lib}")

    if not base_lib.exists():
        raise RuntimeError(f"TK base library not found: {base_lib}")

    print(f"Copying {jni_lib.name} to {resources_dir}")
    shutil.copy2(jni_lib, resources_dir)

    print(f"Copying {base_lib.name} to {resources_dir}")
    shutil.copy2(base_lib, resources_dir)


def main():
    parser = argparse.ArgumentParser(
        description="Build TK Stemmer JNI native library",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python build_native.py /opt/tk-stemmer
  python build_native.py C:\\tk-stemmer
  JAVA_HOME=/usr/lib/jvm/java-21 python build_native.py /opt/tk-stemmer
        """,
    )
    parser.add_argument(
        "tk_stemmer_dir",
        type=Path,
        help="Path to TK Stemmer C++ library installation",
    )
    parser.add_argument(
        "--java-home",
        type=Path,
        help="Path to JDK 21+ (defaults to JAVA_HOME environment variable)",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Clean build directory before building",
    )

    args = parser.parse_args()

    # Resolve paths
    script_dir = Path(__file__).parent.resolve()
    native_dir = script_dir.parent / "src" / "main" / "native"

    try:
        # Detect platform
        platform_name, lib_ext = get_platform_info()
        print(f"Platform: {platform_name}")

        # Find JAVA_HOME
        java_home = args.java_home or find_java_home()
        print(f"JAVA_HOME: {java_home}")

        # Verify TK Stemmer
        tk_dir = args.tk_stemmer_dir.resolve()
        verify_tk_stemmer(tk_dir)
        print(f"TK Stemmer: {tk_dir}")

        # Build directory
        build_dir = native_dir / "build"
        if args.clean and build_dir.exists():
            print(f"Cleaning {build_dir}")
            shutil.rmtree(build_dir)

        # Resources directory
        resources_dir = script_dir.parent / "src" / "main" / "resources" / "native" / platform_name

        print("\n=== Configuring ===")
        run_cmake_configure(build_dir, native_dir, java_home, tk_dir)

        print("\n=== Building ===")
        run_cmake_build(build_dir)

        print("\n=== Copying libraries ===")
        copy_libraries(build_dir, resources_dir, tk_dir, platform_name)

        print("\n=== Build complete ===")
        print(f"Native libraries installed to: {resources_dir}")
        for f in resources_dir.iterdir():
            print(f"  {f.name} ({f.stat().st_size / 1024:.1f} KB)")

    except subprocess.CalledProcessError as e:
        print(f"Error: Command failed with exit code {e.returncode}", file=sys.stderr)
        sys.exit(1)
    except RuntimeError as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
