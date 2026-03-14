#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="$ROOT_DIR/tools"
OPEN_DIR="$TOOLS_DIR/open"
LIB_DIR="$OPEN_DIR/lib"
EMULATOR_DIR="$OPEN_DIR/emulator"
PROGUARD_DIR="$OPEN_DIR/proguard"
JAVA_SETUP_DOC="$ROOT_DIR/docs/java-setup.md"

ECJ_VERSION="3.38.0"
CLDC_VERSION="2.0.4"
MIDP_VERSION="2.0.4"
PROGUARD_VERSION="7.8.2"
MICROEMU_VERSION="2.0.0"

ECJ_JAR="$OPEN_DIR/ecj-${ECJ_VERSION}.jar"
CLDC_JAR="$LIB_DIR/cldcapi11.jar"
MIDP_JAR="$LIB_DIR/midpapi20.jar"
PROGUARD_ZIP="$PROGUARD_DIR/proguard-${PROGUARD_VERSION}.zip"
PROGUARD_HOME="$PROGUARD_DIR/proguard-${PROGUARD_VERSION}"
MICROEMU_JAR="$EMULATOR_DIR/microemulator-app-swing.jar"

ECJ_URL="https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/${ECJ_VERSION}/ecj-${ECJ_VERSION}.jar"
CLDC_URL="https://repo1.maven.org/maven2/org/microemu/cldcapi11/${CLDC_VERSION}/cldcapi11-${CLDC_VERSION}.jar"
MIDP_URL="https://repo1.maven.org/maven2/org/microemu/midpapi20/${MIDP_VERSION}/midpapi20-${MIDP_VERSION}.jar"
PROGUARD_URL="https://github.com/Guardsquare/proguard/releases/download/v${PROGUARD_VERSION}/proguard-${PROGUARD_VERSION}.zip"
MICROEMU_URL="https://repo1.maven.org/maven2/org/microemu/microemulator-app-swing/${MICROEMU_VERSION}/microemulator-app-swing-${MICROEMU_VERSION}.jar"

print_requirements() {
    cat <<MSG
Prerequisites (explicit requirements):
- java, javac, jar (from a JDK; recommended: Temurin/OpenJDK 17)
- curl
- unzip

If java exists but fails to run, install/configure a JDK and ensure:
- java -version
- javac -version

See setup guide: $JAVA_SETUP_DOC
MSG
}

require_command() {
    local cmd="$1"
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "Missing required command: $cmd" >&2
        return 1
    fi
    return 0
}

check_prerequisites() {
    local missing=0

    require_command java || missing=1
    require_command javac || missing=1
    require_command jar || missing=1
    require_command curl || missing=1
    require_command unzip || missing=1

    if ! java -version >/dev/null 2>&1; then
        echo "java is present but runtime is not configured. See $JAVA_SETUP_DOC" >&2
        missing=1
    fi

    if ! javac -version >/dev/null 2>&1; then
        echo "javac is present but compiler is not configured. See $JAVA_SETUP_DOC" >&2
        missing=1
    fi

    if [ "$missing" -ne 0 ]; then
        echo "" >&2
        print_requirements >&2
        exit 1
    fi
}

is_valid_archive() {
    local file="$1"
    unzip -tqq "$file" >/dev/null 2>&1
}

download_archive() {
    local name="$1"
    local url="$2"
    local destination="$3"
    local temp_file="${destination}.download"

    if [ -f "$destination" ] && is_valid_archive "$destination"; then
        echo "$name already present"
        return 0
    fi

    rm -f "$destination" "$temp_file"

    echo "Downloading $name from $url"
    if ! curl -fL --retry 3 --retry-delay 2 --silent --show-error "$url" -o "$temp_file"; then
        echo "Error: failed to download $name." >&2
        rm -f "$temp_file"
        exit 1
    fi

    if ! is_valid_archive "$temp_file"; then
        echo "Error: downloaded $name is not a valid archive." >&2
        rm -f "$temp_file"
        exit 1
    fi

    mv "$temp_file" "$destination"
    echo "Downloaded $name"
}

verify_ecj_compatibility() {
    local temp_dir="$OPEN_DIR/.ecj-compat-check"
    local src_file="$temp_dir/CompatCheck.java"

    rm -rf "$temp_dir"
    mkdir -p "$temp_dir"
    cat > "$src_file" <<'JAVA'
public class CompatCheck {}
JAVA

    if ! java -jar "$ECJ_JAR" -nowarn \
        -source 1.3 -target 1.1 \
        -bootclasspath "$CLDC_JAR:$MIDP_JAR" \
        -classpath "$CLDC_JAR:$MIDP_JAR" \
        -d "$temp_dir" "$src_file" >/dev/null 2>&1; then
        rm -rf "$temp_dir"
        echo "Error: downloaded ECJ does not support -source 1.3/-target 1.1 needed for this J2ME build." >&2
        echo "Delete $ECJ_JAR and re-run make setup." >&2
        exit 1
    fi

    rm -rf "$temp_dir"
}

check_prerequisites

mkdir -p "$LIB_DIR" "$EMULATOR_DIR" "$PROGUARD_DIR"

echo "Installing deterministic J2ME toolchain into $OPEN_DIR"

download_archive "Eclipse ECJ" "$ECJ_URL" "$ECJ_JAR"
download_archive "CLDC API stubs" "$CLDC_URL" "$CLDC_JAR"
download_archive "MIDP API stubs" "$MIDP_URL" "$MIDP_JAR"
verify_ecj_compatibility
download_archive "ProGuard" "$PROGUARD_URL" "$PROGUARD_ZIP"
download_archive "MicroEmulator" "$MICROEMU_URL" "$MICROEMU_JAR"

if [ ! -f "$PROGUARD_HOME/bin/proguard.sh" ]; then
    rm -rf "$PROGUARD_HOME"
    unzip -q "$PROGUARD_ZIP" -d "$PROGUARD_DIR"
fi

echo "Setup complete."
echo "- Compiler: $ECJ_JAR"
echo "- APIs: $CLDC_JAR, $MIDP_JAR"
echo "- Preverify: $PROGUARD_HOME/bin/proguard.sh"
echo "- Emulator: $MICROEMU_JAR"
