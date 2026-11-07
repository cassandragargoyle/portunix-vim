#!/bin/bash
# Wrapper script for fetch_natives.py
# Usage: ./fetch-natives.sh
# Environment: MORPHODITA_VERSION - version to download (default: 1.11.3)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$SCRIPT_DIR/fetch_natives.py" "$@"
