# Wrapper script for fetch_natives.py
# Usage: .\fetch-natives.ps1
# Environment: MORPHODITA_VERSION - version to download (default: 1.11.3)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PythonScript = Join-Path $ScriptDir "fetch_natives.py"

# Try python3 first, then python
try {
    python3 $PythonScript @args
} catch {
    python $PythonScript @args
}
