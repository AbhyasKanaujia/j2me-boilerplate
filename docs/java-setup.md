# Java Setup (Recommended: JDK 17)

This boilerplate expects a working JDK on `PATH`.

## Required commands

- `java`
- `javac`
- `jar`

## Install JDK 17

### macOS (Homebrew)

```bash
brew install --cask temurin@17
```

If your shell still cannot find Java:

```bash
export PATH="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin:$PATH"
```

### Ubuntu/Debian

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
```

### Fedora

```bash
sudo dnf install -y java-17-openjdk-devel
```

### Windows

Install Temurin 17 from Adoptium and reopen your terminal:

- https://adoptium.net/temurin/releases/?version=17

## Verify

Run all of these and make sure they print versions/help without errors:

```bash
java -version
javac -version
jar --help >/dev/null
```

Then return to the project root and run:

```bash
make setup
make build
```
