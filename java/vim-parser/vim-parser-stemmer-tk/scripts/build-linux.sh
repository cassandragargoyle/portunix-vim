#!/bin/bash
#
# Build TK Stemmer JNI native library on Linux
# This is a wrapper script that calls build_native.py
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

python3 "$SCRIPT_DIR/build_native.py" "$@"
