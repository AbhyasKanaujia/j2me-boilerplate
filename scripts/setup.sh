#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="$ROOT_DIR/tools"
WTK_DIR="$TOOLS_DIR/wtk"
WTK_ARCHIVE="$TOOLS_DIR/sun_java_wireless_toolkit-2.5.2_01-linuxi486.bin.sh"
WTK_URL="https://download.oracle.com/otn-pub/java/sun_java_wireless_toolkit/2.5.2_01/sun_java_wireless_toolkit-2.5.2_01-linuxi486.bin.sh"

mkdir -p "$TOOLS_DIR"

if [ -d "$WTK_DIR" ] && [ -x "$WTK_DIR/bin/preverify" ]; then
    echo "J2ME Wireless Toolkit already installed at $WTK_DIR"
    exit 0
fi

if ! command -v unzip >/dev/null 2>&1; then
    echo "Error: 'unzip' is required but not installed." >&2
    exit 1
fi

if [ ! -f "$WTK_ARCHIVE" ]; then
    echo "Downloading J2ME Wireless Toolkit..."
    if command -v wget >/dev/null 2>&1; then
        wget -c --no-cookies --no-check-certificate \
            --header "Cookie: oraclelicense=accept-securebackup-cookie" \
            -O "$WTK_ARCHIVE" \
            "$WTK_URL"
    elif command -v curl >/dev/null 2>&1; then
        curl -L --fail \
            -H "Cookie: oraclelicense=accept-securebackup-cookie" \
            -o "$WTK_ARCHIVE" \
            "$WTK_URL"
    else
        echo "Error: either 'wget' or 'curl' is required to download the toolkit." >&2
        exit 1
    fi
fi

rm -rf "$WTK_DIR"
mkdir -p "$WTK_DIR"

echo "Extracting toolkit..."
unzip -q "$WTK_ARCHIVE" -d "$WTK_DIR"

echo "Setup complete."
