# J2ME Boilerplate

Minimal Java ME project that compiles and runs a MIDlet through a single Makefile interface.

## Features

- J2ME Wireless Toolkit setup script
- End-to-end build pipeline (`compile -> preverify -> jar`)
- Sample `MainMIDlet` that renders a basic form
- Emulator launch with one command

## Repository Layout

- `src/` Java source code
- `build/` transient class/preverify output
- `dist/` generated JAR output
- `tools/` downloaded toolkit files
- `scripts/` setup automation

## Quick Start

```bash
git clone <your-repo-url>
cd j2me-boilerplate
make setup
make run
```

## Build Commands

```bash
make help   # show available commands
make setup  # download/extract J2ME toolkit
make build  # compile + preverify + package
make run    # build and launch emulator
make clean  # remove generated files
```

## Notes

- This boilerplate uses Sun Java Wireless Toolkit 2.5.2.
- `make build` updates `MIDlet-Jar-Size` in `app.jad` automatically.
- The Makefile is the primary abstraction layer so everyday usage stays simple.

## Screenshot

Add an emulator screenshot at `docs/emulator.png`, then uncomment this line:

<!-- ![J2ME emulator screenshot](docs/emulator.png) -->
