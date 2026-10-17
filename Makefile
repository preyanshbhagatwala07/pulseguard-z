# ==============================================================================
# PulseGuard-Z Makefile: IBM LinuxONE & s390x Native Build Automation
# IBM Z Datathon 2026 | Team CodeBuilders
# ==============================================================================

JAVAC := javac
JAVA  := java
TARGET_DIR := target/classes
SRC_DIR := src/main/java
RESOURCES_DIR := src/main/resources/static
PORT := 8080

.PHONY: all build run diagnostics verify stress clean help

all: build

help:
	@echo "PulseGuard-Z Build System (IBM LinuxONE & s390x)"
	@echo "------------------------------------------------"
	@echo "  make build        : Compile all Java enterprise subsystems"
	@echo "  make run          : Launch server on port $(PORT)"
	@echo "  make diagnostics  : Execute 10-point system verification suite"
	@echo "  make verify       : Alias for diagnostics"
	@echo "  make stress       : Run 100k multi-threaded stress harness"
	@echo "  make clean        : Remove compiled bytecode and artifacts"

build:
	@echo "==> Compiling PulseGuard-Z enterprise modules..."
	@mkdir -p $(TARGET_DIR)
	@mkdir -p $(TARGET_DIR)/static
	@cp -r $(RESOURCES_DIR)/* $(TARGET_DIR)/static/
	$(JAVAC) -d $(TARGET_DIR) -sourcepath $(SRC_DIR) \
		$(SRC_DIR)/com/ibmz/pulseguard/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/model/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/engine/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/security/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/storage/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/ha/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/stress/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/abdm/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/clinical/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/audit/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/teleicu/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/report/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/service/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/server/*.java \
		$(SRC_DIR)/com/ibmz/pulseguard/diagnostics/*.java
	@echo "==> Build successful."

run: build
	@echo "==> Starting PulseGuard-Z Tele-ICU Engine on port $(PORT)..."
	$(JAVA) -Xms512m -Xmx2g -cp $(TARGET_DIR) com.ibmz.pulseguard.Main $(PORT)

diagnostics: build
	@echo "==> Executing Enterprise Diagnostics 10-Point Verification Suite..."
	$(JAVA) -cp $(TARGET_DIR) com.ibmz.pulseguard.Main --diagnostics

verify: diagnostics

stress: build
	@echo "==> Executing 100k Multi-Threaded Concurrent Stress Test..."
	$(JAVA) -cp $(TARGET_DIR) com.ibmz.pulseguard.stress.EnterpriseStressHarness 100000 16

clean:
	@echo "==> Cleaning build artifacts..."
	@rm -rf target/
