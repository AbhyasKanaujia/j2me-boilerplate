SHELL := /usr/bin/env bash

OPEN := tools/open
LIB := $(OPEN)/lib
EMULATOR := $(OPEN)/emulator
PROGUARD_HOME := $(OPEN)/proguard/proguard-7.8.2
ECJ_VERSION := 3.38.0

SRC := src
BUILD := build
CLASSES := $(BUILD)/classes
DIST := dist

RAW_JAR := $(BUILD)/app-unverified.jar
PREVERIFIED_JAR := $(BUILD)/app-preverified.jar
JAR := $(DIST)/app.jar
JAD := app.jad

ECJ_JAR := $(OPEN)/ecj-$(ECJ_VERSION).jar
CLDC_JAR := $(LIB)/cldcapi11.jar
MIDP_JAR := $(LIB)/midpapi20.jar
PROGUARD := $(PROGUARD_HOME)/bin/proguard.sh
MICROEMU_JAR := $(EMULATOR)/microemulator-app-swing.jar
JAVA_SETUP_DOC := docs/java-setup.md

ECJ_SOURCE ?= 1.3
ECJ_TARGET ?= 1.1
J2ME_LIBS := $(CLDC_JAR):$(MIDP_JAR)
JAVA_SOURCES := $(wildcard $(SRC)/*.java)

.DEFAULT_GOAL := help

.PHONY: help setup doctor check-java check-build-tools check-emulator compile preverify jar build run clean

help:
	@echo "make setup  - install pinned project toolchain"
	@echo "make build  - compile, preverify, and package the MIDlet"
	@echo "make run    - build and run in MicroEmulator"
	@echo "make doctor - show prerequisites and toolchain status"
	@echo "make clean  - remove generated build artifacts"

setup:
	./scripts/setup.sh

doctor:
	@echo "System prerequisites:"
	@for cmd in java javac jar curl unzip; do \
		if command -v "$$cmd" >/dev/null 2>&1; then \
			echo "  [ok] $$cmd"; \
		else \
			echo "  [missing] $$cmd"; \
		fi; \
	done
	@echo ""
	@echo "Java runtime:"
	@java -version 2>&1 | head -n1 || true
	@echo ""
	@echo "Project toolchain files:"
	@for file in "$(ECJ_JAR)" "$(CLDC_JAR)" "$(MIDP_JAR)" "$(PROGUARD)" "$(MICROEMU_JAR)"; do \
		if [ -e "$$file" ]; then echo "  [ok] $$file"; else echo "  [missing] $$file"; fi; \
	done

check-java:
	@command -v java >/dev/null 2>&1 || (echo "Missing java. Install Temurin/OpenJDK 17. See $(JAVA_SETUP_DOC)." >&2; exit 1)
	@command -v javac >/dev/null 2>&1 || (echo "Missing javac. Install a full JDK (recommended: 17). See $(JAVA_SETUP_DOC)." >&2; exit 1)
	@command -v jar >/dev/null 2>&1 || (echo "Missing jar. Install a full JDK (recommended: 17). See $(JAVA_SETUP_DOC)." >&2; exit 1)
	@java -version >/dev/null 2>&1 || (echo "java exists but runtime is not configured. See $(JAVA_SETUP_DOC)." >&2; exit 1)
	@javac -version >/dev/null 2>&1 || (echo "javac exists but compiler is not configured. See $(JAVA_SETUP_DOC)." >&2; exit 1)

check-build-tools: check-java
	@test -f "$(ECJ_JAR)" || (echo "Missing $(ECJ_JAR). Run 'make setup'." >&2; exit 1)
	@test -f "$(CLDC_JAR)" || (echo "Missing $(CLDC_JAR). Run 'make setup'." >&2; exit 1)
	@test -f "$(MIDP_JAR)" || (echo "Missing $(MIDP_JAR). Run 'make setup'." >&2; exit 1)
	@test -f "$(PROGUARD)" || (echo "Missing $(PROGUARD). Run 'make setup'." >&2; exit 1)

check-emulator: check-java
	@test -f "$(MICROEMU_JAR)" || (echo "Missing $(MICROEMU_JAR). Run 'make setup'." >&2; exit 1)

compile: check-build-tools
	@if [ -z "$(JAVA_SOURCES)" ]; then echo "No Java sources found in $(SRC)/" >&2; exit 1; fi
	@rm -rf "$(CLASSES)"
	@mkdir -p "$(CLASSES)"
	java -jar "$(ECJ_JAR)" -nowarn \
		-source "$(ECJ_SOURCE)" -target "$(ECJ_TARGET)" \
		-bootclasspath "$(J2ME_LIBS)" -classpath "$(J2ME_LIBS)" \
		-d "$(CLASSES)" $(JAVA_SOURCES)

preverify: compile
	@mkdir -p "$(BUILD)"
	jar cf "$(RAW_JAR)" -C "$(CLASSES)" .
	bash "$(PROGUARD)" \
		-injars "$(RAW_JAR)" \
		-outjars "$(PREVERIFIED_JAR)" \
		-libraryjars "$(CLDC_JAR)" \
		-libraryjars "$(MIDP_JAR)" \
		-microedition \
		-dontshrink \
		-dontoptimize \
		-dontobfuscate \
		-dontwarn \
		-keep 'public class * extends javax.microedition.midlet.MIDlet { public *; }'

jar: preverify
	@mkdir -p "$(DIST)"
	cp "$(PREVERIFIED_JAR)" "$(JAR)"
	@JAR_SIZE=$$(wc -c < "$(JAR)" | tr -d '[:space:]'); \
	awk -v size="$$JAR_SIZE" -v jar_url="$(JAR)" 'BEGIN{size_updated=0;url_updated=0} /^MIDlet-Jar-Size:/ {print "MIDlet-Jar-Size: " size; size_updated=1; next} /^MIDlet-Jar-URL:/ {print "MIDlet-Jar-URL: " jar_url; url_updated=1; next} {print} END{if (!url_updated) print "MIDlet-Jar-URL: " jar_url; if (!size_updated) print "MIDlet-Jar-Size: " size}' "$(JAD)" > "$(JAD).tmp"; \
	mv "$(JAD).tmp" "$(JAD)"

build: jar

run: build check-emulator
	java -jar "$(MICROEMU_JAR)" "$(JAD)"

clean:
	rm -rf "$(BUILD)" "$(DIST)"
