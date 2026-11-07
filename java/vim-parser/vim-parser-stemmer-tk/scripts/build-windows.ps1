# Build TK Stemmer JNI native library on Windows
# This is a wrapper script that calls build_native.py

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

python "$ScriptDir\build_native.py" @args
