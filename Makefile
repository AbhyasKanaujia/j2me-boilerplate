WTK := tools/wtk
SRC := src
BUILD := build
CLASSES := $(BUILD)/classes
PREVERIFIED := $(BUILD)/preverified
DIST := dist

JAR := $(DIST)/app.jar
JAD := app.jad

MIDP_API := $(WTK)/lib/midpapi.zip
JAVAC_FLAGS := -target 1.4 -source 1.4

.DEFAULT_GOAL := help

.PHONY: help setup check-wtk compile preverify jar build run clean

help:
	@echo "make setup  - download and extract the J2ME Wireless Toolkit"
	@echo "make build  - compile, preverify, and package the MIDlet"
	@echo "make run    - build and run the MIDlet in the emulator"
	@echo "make clean  - remove generated build artifacts"

setup:
	./scripts/setup.sh

check-wtk:
	@test -f "$(MIDP_API)" || (echo "WTK not found. Run 'make setup' first." >&2; exit 1)

compile: check-wtk
	@mkdir -p "$(CLASSES)"
	javac $(JAVAC_FLAGS) -classpath "$(MIDP_API)" -d "$(CLASSES)" "$(SRC)"/*.java

preverify: compile
	@rm -rf "$(PREVERIFIED)"
	@mkdir -p "$(PREVERIFIED)"
	"$(WTK)/bin/preverify" -classpath "$(MIDP_API):$(CLASSES)" -d "$(PREVERIFIED)" "$(CLASSES)"

jar: preverify
	@mkdir -p "$(DIST)"
	jar cf "$(JAR)" -C "$(PREVERIFIED)" .
	@JAR_SIZE=$$(wc -c < "$(JAR)" | tr -d '[:space:]'); \
	awk -v size="$$JAR_SIZE" 'BEGIN{updated=0} /^MIDlet-Jar-Size:/ {print "MIDlet-Jar-Size: " size; updated=1; next} {print} END{if (!updated) print "MIDlet-Jar-Size: " size}' "$(JAD)" > "$(JAD).tmp"; \
	mv "$(JAD).tmp" "$(JAD)"

build: jar

run: build check-wtk
	"$(WTK)/bin/emulator" -Xdescriptor:"$(JAD)"

clean:
	rm -rf "$(BUILD)" "$(DIST)"
