# Splunk OpenTelemetry Java Agent Manager

A Go-based command-line tool for managing Splunk OpenTelemetry Java agent installations. This tool provides programmatic management of Java agent JAR files with support for installation, uninstallation, rollback, and upgrade operations.

## Features

- **Install**: Download and install the Splunk Java agent JAR
- **Uninstall**: Remove the Java agent from the system
- **Rollback**: Restore previous version from backup
- **Upgrade**: Upgrade to a newer version with automatic backup
- **Backup Management**: Automatic backup creation and restoration
- **Configuration**: Support for YAML configuration files and environment variables

## Installation

### Build from Source

```bash
make build
```

### Install System-wide

```bash
make install
```

## Usage

### Basic Commands

```bash
# Install latest version
./bin/splunk-otel-manager install

# Install specific version
./bin/splunk-otel-manager install --version 1.32.0

# Uninstall
./bin/splunk-otel-manager uninstall

# Rollback to previous version
./bin/splunk-otel-manager rollback

# Upgrade to latest
./bin/splunk-otel-manager upgrade
```

### Configuration Options

```bash
# Specify custom destination folder
./bin/splunk-otel-manager install --dest-folder /opt/my-java-agent

# Set Java home
./bin/splunk-otel-manager install --java-home /usr/lib/jvm/java-11

# Set service name
./bin/splunk-otel-manager install --service-name my-java-service

# Enable verbose logging
./bin/splunk-otel-manager install --verbose
```

### Configuration File

Create a configuration file at `$HOME/.splunk-otel-manager.yaml`:

```yaml
dest_folder: /opt/splunk-java-agent
backup_folder: /opt/splunk-java-agent/backup
agent_version: latest
access_token: your_token_here
otlp_endpoint: https://ingest.us0.signalfx.com/v2/trace/otlp
keep_backup: true
java_home: /usr/lib/jvm/java-11
service_name: my-java-service
```

## Integration with Java Applications

After installation, integrate the agent with your Java application:

```bash
java -javaagent:/opt/splunk-java-agent/splunk-otel-javaagent.jar \
     -Dotel.service.name=my-service \
     -Dotel.resource.attributes=deployment.environment=production \
     -jar your-application.jar
```

## Environment Variables

The tool supports configuration via environment variables:

- `SPLUNK_ACCESS_TOKEN`: Splunk access token
- `SPLUNK_OTLP_ENDPOINT`: OTLP endpoint URL
- `JAVA_HOME`: Java home directory

## Output Format

All operations return JSON output for easy integration with automation tools:

```json
{
  "success": true,
  "message": "Successfully installed Java agent version 1.32.0",
  "operation": "install",
  "version": "1.32.0",
  "path": "/opt/splunk-java-agent/splunk-otel-javaagent.jar",
  "details": {
    "download_url": "https://github.com/signalfx/splunk-otel-java/releases/download/v1.32.0/splunk-otel-javaagent.jar",
    "java_home": "/usr/lib/jvm/java-11",
    "service_name": "my-java-service"
  }
}
```

## Development

### Prerequisites

- Go 1.21 or later

### Building

```bash
# Download dependencies
make deps

# Build binary
make build

# Run tests
make test

# Clean build artifacts
make clean
```

### Cross-compilation

```bash
# Build for all platforms
make build-all

# Build for specific platforms
make build-linux
make build-darwin
make build-windows
```

## License

This project is licensed under the Apache License 2.0.
