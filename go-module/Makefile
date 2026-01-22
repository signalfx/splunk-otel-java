# Makefile for Splunk OpenTelemetry Java Agent Manager

# Go parameters
GOCMD=go
GOBUILD=$(GOCMD) build
GOCLEAN=$(GOCMD) clean
GOTEST=$(GOCMD) test
GOGET=$(GOCMD) get
GOMOD=$(GOCMD) mod

# Binary name and paths
BINARY_NAME=splunk-otel-manager
BINARY_PATH=bin/$(BINARY_NAME)
MAIN_PATH=cmd/splunk-otel-manager
ANSIBLE_BIN_DIR=../ansible/files/bin

# Build flags
LDFLAGS=-ldflags "-w -s"

.PHONY: all build clean test deps tidy ansible-binaries

all: clean deps build

build:
	mkdir -p bin
	$(GOBUILD) $(LDFLAGS) -o $(BINARY_PATH) ./$(MAIN_PATH)

clean:
	$(GOCLEAN)
	rm -rf bin/

test:
	$(GOTEST) -v ./...

deps:
	$(GOMOD) download

tidy:
	$(GOMOD) tidy

install: build
	sudo cp $(BINARY_PATH) /usr/local/bin/

uninstall:
	sudo rm -f /usr/local/bin/$(BINARY_NAME)

# Cross compilation
build-linux:
	mkdir -p bin
	CGO_ENABLED=0 GOOS=linux GOARCH=amd64 $(GOBUILD) $(LDFLAGS) -o bin/$(BINARY_NAME)-linux-amd64 ./$(MAIN_PATH)

build-darwin-amd64:
	mkdir -p bin
	CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 $(GOBUILD) $(LDFLAGS) -o bin/$(BINARY_NAME)-darwin-amd64 ./$(MAIN_PATH)

build-darwin-arm64:
	mkdir -p bin
	CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 $(GOBUILD) $(LDFLAGS) -o bin/$(BINARY_NAME)-darwin-arm64 ./$(MAIN_PATH)

build-windows:
	mkdir -p bin
	CGO_ENABLED=0 GOOS=windows GOARCH=amd64 $(GOBUILD) $(LDFLAGS) -o bin/$(BINARY_NAME)-windows-amd64.exe ./$(MAIN_PATH)

build-all: build-linux build-darwin-amd64 build-darwin-arm64 build-windows

# Build and copy binaries to ansible/files/bin for Ansible deployment
ansible-binaries: build-all
	@echo "Copying binaries to Ansible files directory..."
	mkdir -p $(ANSIBLE_BIN_DIR)
	cp bin/$(BINARY_NAME)-linux-amd64 $(ANSIBLE_BIN_DIR)/
	cp bin/$(BINARY_NAME)-darwin-amd64 $(ANSIBLE_BIN_DIR)/
	cp bin/$(BINARY_NAME)-darwin-arm64 $(ANSIBLE_BIN_DIR)/
	cp bin/$(BINARY_NAME)-windows-amd64.exe $(ANSIBLE_BIN_DIR)/
	@echo "Binaries copied to $(ANSIBLE_BIN_DIR)"
