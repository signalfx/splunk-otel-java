# Splunk OpenTelemetry Java Agent - Ansible Deployment

This directory contains Ansible playbooks and tools for deploying and managing the Splunk OpenTelemetry Java agent across multiple hosts.

## Structure

```
ansible/
├── ansible.cfg              # Ansible configuration
├── playbooks/
│   └── splunk.yml          # Main playbook for agent lifecycle management
└── README.md               # This file
```

## Prerequisites

- Ansible 2.9 or later
- Java 11 or later (on target hosts)
- SSH access to target hosts (for remote deployment)

## Quick Start

### 1. Run the Playbook

#### Local Installation

```bash
cd playbooks
ansible-playbook splunk.yml -i localhost, --connection=local --ask-become-pass
```

#### Remote Installation

```bash
cd playbooks
ansible-playbook splunk.yml -i your-inventory.yml --ask-become-pass
```


## Playbook Operations

The main playbook (`playbooks/splunk.yml`) performs the following operations in sequence:

1. **Install** - Downloads and installs the Splunk Java agent
2. **Upgrade** - Upgrades to a newer version (with automatic backup)
3. **Rollback** - Restores the previous version from backup
4. **Uninstall** - Removes the agent (optionally keeping backups)

### Customizing Variables

You can override default variables when running the playbook:

```bash
ansible-playbook splunk.yml \
  -i localhost, \
  --connection=local \
  -e agent_version="1.32.0" \
  -e splunk_dest_folder="/custom/path" \
  -e splunk_access_token="your-token" \
  -e otel_exporter_otlp_endpoint="https://ingest.us1.signalfx.com/v2/trace"
```

### Available Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `agent_version` | `latest` | Version of the Java agent to install |
| `splunk_access_token` | `""` | Splunk access token |
| `otel_exporter_otlp_endpoint` | `""` | OTLP endpoint URL |
| `splunk_dest_folder` | `/opt/splunk-java-agent` | Installation directory |
| `keep_backup` | `true` | Keep backup files after operations |
| `manager_repo` | `signalfx/splunk-otel-java` | GitHub repo that hosts the manager releases |
| `manager_version` | `latest` | Manager release version (or `latest`) |
| `manager_download_url_override` | `""` | Override the manager binary download URL |
| `manager_checksum_override` | `""` | Override the manager SHA256 checksum |
| `manager_checksum_url_override` | `""` | Override the manager checksum URL |

## Architecture

This implementation follows the pattern established in `splunk-otel-js`:

1. **Go Binary Manager** - A cross-platform Go binary (`splunk-otel-manager`) handles agent installation, upgrade, rollback, and uninstall operations.

2. **Ansible Playbook** - The playbook detects the target OS/architecture, downloads the appropriate binary, and executes the lifecycle operations.

3. **No Custom Modules** - Unlike previous implementations, this approach doesn't require custom Ansible modules or roles, making it simpler and more maintainable.

## Comparison with Previous Implementation

| Feature | Old Implementation | New Implementation |
|---------|-------------------|-------------------|
| Custom Ansible Module | ✅ Required | ❌ Not needed |
| Ansible Roles | ✅ Complex role structure | ❌ Simple playbook |
| Inventory Files | ✅ Multiple inventory files | ❌ Use standard Ansible inventory |
| SSH Setup Scripts | ✅ Included | ❌ Use standard SSH |
| Docker Compose | ✅ For testing | ❌ Direct testing |
| Multi-platform Support | ✅ Via module | ✅ Via GitHub releases |

## Using with Your Java Application

After installing the agent, instrument your Java application:

### Method 1: Command Line

```bash
java -javaagent:/opt/splunk-java-agent/splunk-otel-javaagent.jar \
     -Dotel.service.name=my-service \
     -Dotel.resource.attributes=deployment.environment=production \
     -Dsplunk.access.token=YOUR_TOKEN \
     -Dotel.exporter.otlp.endpoint=https://ingest.us1.signalfx.com/v2/trace \
     -jar your-application.jar
```

### Method 2: Environment Variables

```bash
export JAVA_TOOL_OPTIONS="-javaagent:/opt/splunk-java-agent/splunk-otel-javaagent.jar"
export OTEL_SERVICE_NAME="my-service"
export SPLUNK_ACCESS_TOKEN="YOUR_TOKEN"
export OTEL_EXPORTER_OTLP_ENDPOINT="https://ingest.us1.signalfx.com/v2/trace"

java -jar your-application.jar
```

## Troubleshooting

### Agent Not Loading

1. Verify the agent file exists: `ls -lh /opt/splunk-java-agent/splunk-otel-javaagent.jar`
2. Check Java version: `java -version` (requires Java 11+)
3. Verify the javaagent path is correct in your command

### No Traces Appearing

1. Check your Splunk access token is valid
2. Verify the OTLP endpoint is reachable
3. Ensure your application is generating HTTP traffic
4. Check application logs for OpenTelemetry initialization messages


## Support

For issues or questions:
- Check the sample application in `sample-app/`
- Review the E2E test script for examples
- Consult the Go module documentation in `../go-module/README.md`
