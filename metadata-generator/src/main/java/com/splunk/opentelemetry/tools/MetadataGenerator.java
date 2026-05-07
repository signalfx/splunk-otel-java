/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class MetadataGenerator {
  private static final String BUNDLED_METRIC =
      "APM bundled, if data points for the metric contain `telemetry.sdk.language` attribute.";
  private static final String CUSTOM_METRIC = "Custom metric.";

  // java instrumentation metrics marked as bundled by mts-category-generator
  private static final Set<String> bundledMetrics =
      new HashSet<>(
          Arrays.asList(
              "http.client.request.duration",
              "http.server.request.duration",
              "jvm.class.loaded",
              "jvm.class.unloaded",
              "jvm.class.count",
              "jvm.cpu.time",
              "jvm.cpu.count",
              "jvm.cpu.recent_utilization",
              "jvm.gc.duration",
              "jvm.memory.used",
              "jvm.memory.committed",
              "jvm.memory.limit",
              "jvm.memory.used_after_last_gc",
              "jvm.thread.count"));

  private static String getRequiredSystemProperty(String name) {
    String value = System.getProperty(name);
    if (value == null) {
      throw new IllegalStateException("System property '" + name + "' must be set");
    }
    return value;
  }

  private static String toAlphaVersion(String version) {
    if (version.endsWith("-SNAPSHOT")) {
      return version.substring(0, version.length() - "-SNAPSHOT".length()) + "-alpha-SNAPSHOT";
    }
    return version + "-alpha";
  }

  public static void main(String[] args) throws IOException {
    String splunkJavaVersion = getRequiredSystemProperty("splunkAgentVersion");
    String otelJavaInstrumentationVersion = getRequiredSystemProperty("otelInstrumentationVersion");
    String otelJavaInstrumentationAlphaVersion = toAlphaVersion(otelJavaInstrumentationVersion);
    String otelJavaVersion = getRequiredSystemProperty("otelVersion");
    String otelJavaAlphaVersion = toAlphaVersion(otelJavaVersion);
    String otelJavaContribVersion = getRequiredSystemProperty("otelContribVersion");
    String otelJavaContribAlphaVersion = toAlphaVersion(otelJavaContribVersion);
    String outputPath = getRequiredSystemProperty("outputPath");

    System.err.println("splunkAgentVersion " + splunkJavaVersion);
    System.err.println(
        "otelInstrumentationVersion "
            + otelJavaInstrumentationVersion
            + "/"
            + otelJavaInstrumentationAlphaVersion);
    System.err.println("otelVersion " + otelJavaVersion + "/" + otelJavaAlphaVersion);
    System.err.println(
        "otelContribVersion " + otelJavaContribVersion + "/" + otelJavaContribAlphaVersion);
    System.err.println("outputPath " + outputPath);

    Map<String, Object> root = new LinkedHashMap<>();
    root.put("component", "Splunk Distribution of OpenTelemetry Java");
    root.put("version", splunkJavaVersion);

    List<Map<String, Object>> dependencies = new ArrayList<>();
    root.put("dependencies", dependencies);

    dependencies.add(
        dependency(
            "OpenTelemetry Java",
            "https://github.com/open-telemetry/opentelemetry-java",
            otelJavaVersion,
            Stability.STABLE));
    dependencies.add(
        dependency(
            "OpenTelemetry Instrumentation for Java",
            "https://github.com/open-telemetry/opentelemetry-java-instrumentation",
            otelJavaInstrumentationVersion,
            Stability.STABLE));
    dependencies.add(
        dependency(
            "OpenTelemetry Java Contrib Resource Providers",
            "https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/resource-providers",
            otelJavaContribAlphaVersion,
            Stability.EXPERIMENTAL));
    dependencies.add(
        dependency(
            "OpenTelemetry Java Contrib Samplers",
            "https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/samplers",
            otelJavaContribAlphaVersion,
            Stability.EXPERIMENTAL));

    List<Map<String, Object>> settings = new ArrayList<>();
    root.put("settings", settings);

    settings.addAll(parseSdkConfiguration());

    // File Configuration
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#file-configuration

    /*
    otel.config.file	OTEL_CONFIG_FILE	The path to the SDK configuration file. Defaults to unset.
     */

    settings.add(
        setting(
            "otel.config.file",
            "The path to the SDK configuration file. Defaults to unset.",
            "",
            SettingType.STRING,
            SettingCategory.GENERAL));

    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/

    // Configuration file
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#configuration-file

    /*
    You can provide a path to agent configuration file by setting the following property:

    System property: otel.javaagent.configuration-file
    Environment variable: OTEL_JAVAAGENT_CONFIGURATION_FILE
    Description: Path to valid Java properties file which contains the agent configuration.
     */

    settings.add(
        setting(
            "otel.javaagent.configuration-file",
            "Path to valid Java properties file which contains the agent configuration.",
            "",
            SettingType.STRING,
            SettingCategory.GENERAL));

    // Extensions
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#extensions

    /*
    You can enable extensions by setting the following property:

    System property: otel.javaagent.extensions
    Environment variable: OTEL_JAVAAGENT_EXTENSIONS
    Description: Path to an extension jar file or folder, containing jar files. If pointing to a folder, every jar file in that folder will be treated as separate, independent extension.
     */

    settings.add(
        setting(
            "otel.javaagent.extensions",
            "Path to an extension jar file or folder, containing jar files. If pointing to a folder, every jar file in that folder will be treated as separate, independent extension.",
            "",
            SettingType.STRING,
            SettingCategory.GENERAL));

    // Java agent logging output
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#java-agent-logging-output

    /*
    The agent’s logging output can be configured by setting the following property:

    System property: otel.javaagent.logging
    Environment variable: OTEL_JAVAAGENT_LOGGING
    Description: The Java agent logging mode. The following 3 modes are supported:

    simple: The agent will print out its logs using the standard error stream. Only INFO or higher logs will be printed. This is the default Java agent logging mode.
    none: The agent will not log anything - not even its own version.
    application: The agent will attempt to redirect its own logs to the instrumented application's slf4j logger. This works the best for simple one-jar applications that do not use multiple classloaders; Spring Boot apps are supported as well. The Java agent output logs can be further configured using the instrumented application's logging configuration (e.g. logback.xml or log4j2.xml). Make sure to test that this mode works for your application before running it in a production environment.
     */

    settings.add(
        setting(
            "otel.javaagent.logging",
            "The Java agent logging mode. The following 3 modes are supported:\n"
                + "simple: The agent will print out its logs using the standard error stream. Only INFO or higher logs will be printed. This is the default Java agent logging mode.\n"
                + "none: The agent will not log anything - not even its own version.\n"
                + "application: The agent will attempt to redirect its own logs to the instrumented application's slf4j logger. This works the best for simple one-jar applications that do not use multiple classloaders; Spring Boot apps are supported as well. The Java agent output logs can be further configured using the instrumented application's logging configuration (e.g. logback.xml or log4j2.xml). Make sure to test that this mode works for your application before running it in a production environment.",
            "simple",
            SettingType.STRING,
            SettingCategory.GENERAL));

    // Peer service name
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#peer-service-name

    /*
    System property: otel.instrumentation.common.peer-service-mapping
    Environment variable: OTEL_INSTRUMENTATION_COMMON_PEER_SERVICE_MAPPING
    Description: Used to specify a mapping from host names or IP addresses to peer services, as a comma-separated list of &lt;host_or_ip&gt;=&lt;user_assigned_name&gt; pairs. The peer service is added as an attribute to a span whose host or IP address match the mapping.
     */

    settings.add(
        setting(
            "otel.instrumentation.common.peer-service-mapping",
            "Used to specify a mapping from host names or IP addresses to peer services, as a comma-separated list of &lt;host_or_ip&gt;=&lt;user_assigned_name&gt; pairs. The peer service is added as an attribute to a span whose host or IP address match the mapping.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // DB statement sanitization
    // https://opentelemetry.io/docs/zero-code/java/agent/instrumentation/#db-statement-sanitization

    /*
    System property: otel.instrumentation.common.db-statement-sanitizer.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED
    Default: true
    Description: Enables the DB statement sanitization.
     */

    settings.add(
        setting(
            "otel.instrumentation.common.db.query-sanitization.enabled",
            "Enables the DB query sanitization.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // Capturing HTTP request and response headers
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#capturing-http-request-and-response-headers

    /*
    System property: otel.instrumentation.http.client.capture-request-headers
    Environment variable: OTEL_INSTRUMENTATION_HTTP_CLIENT_CAPTURE_REQUEST_HEADERS
    Description: A comma-separated list of HTTP header names. HTTP client instrumentations will capture HTTP request header values for all configured header names.

    System property: otel.instrumentation.http.client.capture-response-headers
    Environment variable: OTEL_INSTRUMENTATION_HTTP_CLIENT_CAPTURE_RESPONSE_HEADERS
    Description: A comma-separated list of HTTP header names. HTTP client instrumentations will capture HTTP response header values for all configured header names.

    System property: otel.instrumentation.http.server.capture-request-headers
    Environment variable: OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_REQUEST_HEADERS
    Description: A comma-separated list of HTTP header names. HTTP server instrumentations will capture HTTP request header values for all configured header names.

    System property: otel.instrumentation.http.server.capture-response-headers
    Environment variable: OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_RESPONSE_HEADERS
    Description: A comma-separated list of HTTP header names. HTTP server instrumentations will capture HTTP response header values for all configured header names.
     */

    settings.add(
        setting(
            "otel.instrumentation.http.client.capture-request-headers",
            "A comma-separated list of HTTP header names. HTTP client instrumentations will capture HTTP request header values for all configured header names.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.http.client.capture-response-headers",
            "A comma-separated list of HTTP header names. HTTP client instrumentations will capture HTTP response header values for all configured header names.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.http.server.capture-request-headers",
            "A comma-separated list of HTTP header names. HTTP server instrumentations will capture HTTP request header values for all configured header names.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.http.server.capture-response-headers",
            "A comma-separated list of HTTP header names. HTTP server instrumentations will capture HTTP response header values for all configured header names.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.sanitization.url.experimental.sensitive-query-parameters",
            "A comma-separated list of HTTP url parameter names. HTTP server and client instrumentations will redact the values of give url parameters.",
            "AWSAccessKeyId, Signature, sig, X-Goog-Signature",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.http.known-methods",
            "A comma-separated list of known HTTP methods. `_OTHER` will be used for methods not in this list.",
            "CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // Capturing servlet request parameters
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#capturing-servlet-request-parameters

    /*
    System property: otel.instrumentation.servlet.experimental.capture-request-parameters
    Environment variable: OTEL_INSTRUMENTATION_SERVLET_EXPERIMENTAL_CAPTURE_REQUEST_PARAMETERS
    Description: A comma-separated list of request parameter names.
     */

    settings.add(
        setting(
            "otel.instrumentation.servlet.experimental.capture-request-parameters",
            "A comma-separated list of request parameter names.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // Capturing consumer message receive telemetry in messaging instrumentations
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#capturing-consumer-message-receive-telemetry-in-messaging-instrumentations

    /*
    You can configure the agent to capture the consumer message receive telemetry in messaging instrumentation. Use the following property to enable it:

    System property: otel.instrumentation.messaging.experimental.receive-telemetry.enabled
    Environment variable: OTEL_INSTRUMENTATION_MESSAGING_EXPERIMENTAL_RECEIVE_TELEMETRY_ENABLED
    Default: false
    Description: Enables the consumer message receive telemetry.

    Note that this will cause the consumer side to start a new trace, with only a span link connecting it to the producer trace.
     */

    settings.add(
        setting(
            "otel.instrumentation.messaging.experimental.receive-telemetry.enabled",
            "Enables the consumer message receive telemetry. Note that this will cause the consumer side to start a new trace, with only a span link connecting it to the producer trace.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // Capturing enduser attributes
    // https://opentelemetry.io/docs/languages/java/automatic/configuration/#capturing-enduser-attributes

    /*
    You can configure the agent to capture general identity attributes (enduser.id, enduser.role, enduser.scope) from instrumentation libraries like JavaEE/JakartaEE Servlet and Spring Security.

    Note: Given the sensitive nature of the data involved, this feature is turned off by default while allowing selective activation for particular attributes. You must carefully evaluate each attribute’s privacy implications before enabling the collection of the data.

    System property: otel.instrumentation.common.enduser.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_ENDUSER_ENABLED
    Default: false
    Description: Common flag for enabling/disabling enduser attributes.

    System property: otel.instrumentation.common.enduser.id.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_ENDUSER_ID_ENABLED
    Default: false
    Description: Determines whether to capture enduser.id semantic attribute.

    System property: otel.instrumentation.common.enduser.role.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_ENDUSER_ROLE_ENABLED
    Default: false
    Description: Determines whether to capture enduser.role semantic attribute.

    System property: otel.instrumentation.common.enduser.scope.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_ENDUSER_SCOPE_ENABLED
    Default: false
    Description: Determines whether to capture enduser.scope semantic attribute.
     */

    settings.add(
        setting(
            "otel.instrumentation.common.enduser.enabled",
            "Common flag for enabling/disabling enduser attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.enduser.id.enabled",
            "Determines whether to capture `enduser.id` semantic attribute.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.enduser.role.enabled",
            "Determines whether to capture `enduser.role` semantic attribute.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.enduser.scope.enabled",
            "Determines whether to capture `enduser.scope` semantic attribute.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // the following don't seem to be documented anywhere
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/1cddf1b4a7dac1ada37f356cd5e25f1ca16e2395/javaagent-extension-api/src/main/java/io/opentelemetry/javaagent/bootstrap/internal/CommonConfig.java#L60

    settings.add(
        setting(
            "otel.instrumentation.http.client.emit-experimental-telemetry",
            "Enables experimental http client telemetry.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.http.server.emit-experimental-telemetry",
            "Enables experimental http server telemetry.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // Enable only specific instrumentation
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#enable-only-specific-instrumentation

    /*
    You can disable all default auto instrumentation and selectively re-enable individual instrumentation. This may be desirable to reduce startup overhead or to have more control of which instrumentation is applied.

    Disable all instrumentation in the agent using -Dotel.instrumentation.common.default-enabled=false (or using the equivalent environment variable OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED=false).
    Enable each desired instrumentation individually using -Dotel.instrumentation.[name].enabled=true (or using the equivalent environment variable OTEL_INSTRUMENTATION_[NAME]_ENABLED) where [name] ([NAME]) is the corresponding instrumentation name below.
    Note: Some instrumentation relies on other instrumentation to function properly. When selectively enabling instrumentation, be sure to enable the transitive dependencies too. Determining this dependency relationship is left as an exercise to the user.
     */

    settings.add(
        setting(
            "otel.instrumentation.common.default-enabled",
            "Disable all instrumentations in the agent.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // Suppressing specific agent instrumentation
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#suppressing-specific-agent-instrumentation

    /*
    You can suppress agent instrumentation of specific libraries by using -Dotel.instrumentation.[name].enabled=false (or using the equivalent environment variable OTEL_INSTRUMENTATION_[NAME]_ENABLED) where name (NAME) is the corresponding instrumentation name
     */

    settings.add(
        setting(
            "otel.instrumentation.{name}.enabled",
            "Suppresses agent instrumentation of specific library where `{name}` is the corresponding instrumentation name.",
            "",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // Suppressing controller and/or view spans
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#suppressing-controller-andor-view-spans

    /*
    Some instrumentations (e.g. Spring Web MVC instrumentation) produce SpanKind.Internal spans to capture the controller and/or view execution. These spans can be suppressed using the configuration settings below, without suppressing the entire instrumentation which would also disable the instrumentation’s capturing of http.route and associated span name on the parent SpanKind.Server span.

    System property: otel.instrumentation.common.experimental.controller-telemetry.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_EXPERIMENTAL_CONTROLLER_TELEMETRY_ENABLED
    Default: false
    Description: Enables the controller telemetry.

    System property: otel.instrumentation.common.experimental.view-telemetry.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_EXPERIMENTAL_VIEW_TELEMETRY_ENABLED
    Default: false
    Description: Enables the view telemetry.
     */

    settings.add(
        setting(
            "otel.instrumentation.common.experimental.controller-telemetry.enabled",
            "Enables the controller telemetry.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.experimental.view-telemetry.enabled",
            "Enables the view telemetry.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // Instrumentation span suppression behavior
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#instrumentation-span-suppression-behavior

    /*
    System property: otel.instrumentation.experimental.span-suppression-strategy
    Environment variable: OTEL_INSTRUMENTATION_EXPERIMENTAL_SPAN_SUPPRESSION_STRATEGY
    Description: The Java agent span suppression strategy. The following 3 strategies are supported:

    semconv: The agent will suppress duplicate semantic conventions. This is the default behavior of the Java agent.
    span-kind: The agent will suppress spans with the same kind (except INTERNAL).
    none: The agent will not suppress anything at all. We do not recommend using this option for anything other than debug purposes, as it generates lots of duplicate telemetry data.
     */

    settings.add(
        setting(
            "otel.instrumentation.experimental.span-suppression-strategy",
            "The Java agent span suppression strategy. The following 3 strategies are supported:\n"
                + "semconv: The agent will suppress duplicate semantic conventions. This is the default behavior of the Java agent.\n"
                + "span-kind: The agent will suppress spans with the same kind (except INTERNAL).\n"
                + "none: The agent will not suppress anything at all. We do not recommend using this option for anything other than debug purposes, as it generates lots of duplicate telemetry data.\n",
            "semconv",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/advanced-configuration-options.md

    /*
    | otel.javaagent.exclude-classes | OTEL_JAVAAGENT_EXCLUDE_CLASSES | Suppresses all instrumentation for specific classes, format is "my.package.MyClass,my.package2.\*" |
    | otel.javaagent.exclude-class-loaders | OTEL_JAVAAGENT_EXCLUDE_CLASS_LOADERS | Ignore the specified class loaders, format is "my.package.MyClass,my.package2." |
    | otel.javaagent.experimental.security-manager-support.enabled | OTEL_JAVAAGENT_EXPERIMENTAL_SECURITY_MANAGER_SUPPORT_ENABLED | Grant all privileges to agent code[1] |

    [1] Disclaimer: agent can provide application means for escaping security manager sandbox. Do not use
    this option if your application relies on security manager to run untrusted code.
     */

    settings.add(
        setting(
            "otel.javaagent.exclude-classes",
            "Suppresses all instrumentation for specific classes, format is \"my.package.MyClass,my.package2.*\".",
            "",
            SettingType.STRING,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.javaagent.exclude-class-loaders",
            "Ignore the specified class loaders, format is \"my.package.MyClass,my.package2.\".",
            "",
            SettingType.STRING,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.javaagent.experimental.security-manager-support.enabled",
            "Grant all privileges to agent code. Disclaimer: agent can provide application means for escaping security manager sandbox. Do not use"
                + "this option if your application relies on security manager to run untrusted code.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.GENERAL));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/advanced-configuration-options.md#javascript-snippet-injection
    /*
    | otel.experimental.javascript-snippet | OTEL_EXPERIMENTAL_JAVASCRIPT_SNIPPET | Experimental setting to inject a JavaScript snippet into HTML responses after the opening `<head>` tag. The value should be a complete JavaScript snippet including `<script>` tags if needed, e.g. `-Dotel.experimental.javascript-snippet="<script>console.log('Hello world!');</script>"` |
     */
    settings.add(
        setting(
            "otel.experimental.javascript-snippet",
            "Experimental setting to inject a JavaScript snippet into servlet HTML responses after the opening `<head>` tag.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // Enable Resource Providers that are disabled by default
    // https://opentelemetry.io/docs/languages/java/automatic/configuration/#enable-resource-providers-that-are-disabled-by-default

    /*
    System property: otel.resource.providers.aws.enabled
    Environment variable: OTEL_RESOURCE_PROVIDERS_AWS_ENABLED
    Default: false
    Description: Enables the AWS Resource Provider.

    System property: otel.resource.providers.gcp.enabled
    Environment variable: OTEL_RESOURCE_PROVIDERS_GCP_ENABLED
    Default: false
    Description: Enables the GCP Resource Provider.

    System property: otel.resource.providers.azure.enabled
    Environment variable: OTEL_RESOURCE_PROVIDERS_AZURE_ENABLED
    Default: false
    Description: Enables the Azure Resource Provider.
     */

    settings.add(
        setting(
            "otel.resource.providers.aws.enabled",
            "Enables the AWS Resource Provider.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.RESOURCE_PROVIDER));
    settings.add(
        setting(
            "otel.resource.providers.gcp.enabled",
            "Enables the GCP Resource Provider.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.RESOURCE_PROVIDER));
    settings.add(
        setting(
            "otel.resource.providers.azure.enabled",
            "Enables the Azure Resource Provider.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.RESOURCE_PROVIDER));

    // https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md

    // Splunk configuration
    // https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#splunk-configuration

    /*
    | `splunk.access.token`                   | `SPLUNK_ACCESS_TOKEN`                   | unset                   | Stable       | (Optional) Auth token allowing exporters to communicate directly with the Splunk cloud, passed as `X-SF-TOKEN` header. Currently, the [SignalFx metrics exporter](metrics.md) supports this property.                |
    | `splunk.realm`                          | `SPLUNK_REALM`                          | `none`                  | Stable       | Specify your organization's realm name, such as `us0` or `us1. When the realm is set, telemetry data is sent directly to the ingest endpoint of Splunk Observability Cloud, bypassing the Splunk Distribution of the OpenTelemetry Collector. If no realm is specified, telemetry is routed to a Splunk OpenTelemetry Collector deployed locally. This configuration applies only to metrics and traces. |
    | `splunk.metrics.force_full_commandline` | `SPLUNK_METRICS_FORCE_FULL_COMMANDLINE` | `false`                 | Experimental | Adds the full command line as a resource attribute for all metrics. If false, commands longer than 255 characters are truncated.                                                                                     |
    | `splunk.trace-response-header.enabled`  | `SPLUNK_TRACE_RESPONSE_HEADER_ENABLED`  | `true`                  | Stable       | Enables adding server trace information to HTTP response headers. See [this document](server-trace-info.md) for more information.                                                                                    |
     */

    settings.add(
        setting(
            "splunk.access.token",
            "(Optional) Auth token allowing exporters to communicate directly with the Splunk cloud, passed as X-SF-TOKEN header.",
            "",
            SettingType.STRING,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "splunk.realm",
            "Specify your organization's realm name, such as `us0` or `us1. When the realm is set, telemetry data is sent directly to the ingest endpoint of Splunk Observability Cloud, bypassing the Splunk Distribution of the OpenTelemetry Collector. If no realm is specified, telemetry is routed to a Splunk OpenTelemetry Collector deployed locally. This configuration applies only to metrics and traces.",
            "none",
            SettingType.STRING,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "splunk.metrics.force_full_commandline",
            "Adds the full command line as a resource attribute for all metrics. If false, commands longer than 255 characters are truncated.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "splunk.trace-response-header.enabled",
            "Enables adding server trace information to HTTP response headers. [See this document](https://docs.splunk.com/observability/en/gdi/get-data-in/application/java/configuration/advanced-java-otel-configuration.html#server-trace-information) for more information.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.GENERAL));

    // Trace configuration
    // https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#trace-configuration

    /*
    otel.instrumentation.methods.include	OTEL_INSTRUMENTATION_METHODS_INCLUDE	unset	Stable	Same as adding @WithSpan annotation functionality for the target method string.
    Format
    my.package.MyClass1[method1,method2];my.package.MyClass2[method3]
    otel.instrumentation.opentelemetry-annotations.exclude-methods	OTEL_INSTRUMENTATION_OPENTELEMETRY_ANNOTATIONS_EXCLUDE_METHODS	unset	Stable	Suppress @WithSpan instrumentation for specific methods.
    Format
    my.package.MyClass1[method1,method2];my.package.MyClass2[method3]
     */

    settings.add(
        setting(
            "otel.instrumentation.methods.include",
            "Same as adding @WithSpan annotation functionality for the target method string, e.g. my.package.MyClass1[method1,method2];my.package.MyClass2[method3]",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    settings.add(
        setting(
            "otel.instrumentation.opentelemetry-annotations.exclude-methods",
            "Suppress @WithSpan instrumentation for specific methods, e.g. my.package.MyClass1[method1,method2];my.package.MyClass2[method3]",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // Profiler settings
    // https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#profiler-settings

    /*
    | `splunk.profiler.enabled`                 | false                         | set to `true` to enable the profiler                                                                                      |
    | `splunk.profiler.directory`               | system temp directory         | location of JFR files, defaults to `System.getProperty("java.io.tmpdir")`                                                 |
    | `splunk.profiler.recording.duration`      | 20s                           | recording unit duration                                                                                                   |
    | `splunk.profiler.keep-files`              | false                         | leave JFR files on disk if `true`                                                                                         |
    | `splunk.profiler.logs-endpoint`           | http://localhost:4318/v1/logs | where to send OTLP logs, defaults to `otel.exporter.otlp.endpoint`                                                        |
    | `splunk.profiler.call.stack.interval`     | 10000ms                       | how often to sample call stacks                                                                                           |
    | `splunk.profiler.memory.enabled`          | false                         | set to `true` to enable all other memory profiling options unless explicitly disabled. Setting to `true` enables metrics. |
    | `splunk.profiler.memory.event.rate`       | 150/s                         | allocation event rate                                                                                                     |
    | `splunk.profiler.include.internal.stacks` | false                         | set to `true` to include stack traces of agent internal threads and stack traces with only JDK internal frames            |
    | `splunk.profiler.tracing.stacks.only`     | false                         | set to `true` to include only stack traces that are linked to a span context                                              |
    | `splunk.profiler.otlp.protocol`           | `http/protobuf`               | The transport protocol to use on profiling OTLP log requests. Options include `grpc` and `http/protobuf`.                 |
     */

    settings.add(
        setting(
            "splunk.profiler.enabled",
            "Enables cpu profiler.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.directory",
            "Location of JFR files, defaults to System.getProperty(\"java.io.tmpdir\").",
            "value of System.getProperty(\"java.io.tmpdir\")",
            SettingType.STRING,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.recording.duration",
            "Recording unit duration.",
            "20s",
            SettingType.STRING,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.keep-files",
            "Leave JFR files on disk if true.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.logs-endpoint",
            "Where to send OTLP logs, defaults to `otel.exporter.otlp.endpoint.",
            "http://localhost:4318/v1/logs",
            SettingType.STRING,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.call.stack.interval",
            "How often to sample call stacks.",
            "10000ms",
            SettingType.STRING,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.memory.enabled",
            "Enables allocation profiler.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.memory.event.rate",
            "Allocation event rate.",
            "150/s",
            SettingType.STRING,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.include.internal.stacks",
            "Set to `true` to include stack traces of agent internal threads and stack traces with only JDK internal frames.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.tracing.stacks.only",
            "Set to `true` to include only stack traces that are linked to a span context.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.PROFILER));
    settings.add(
        setting(
            "splunk.profiler.otlp.protocol",
            "The transport protocol to use on profiling OTLP log requests. Options include grpc and http/protobuf.",
            "http/protobuf",
            SettingType.STRING,
            SettingCategory.PROFILER));

    // metrics from the Java SDK, .NET SDK does not have built-in metrics so this is not present in
    // their yaml
    List<Map<String, Object>> metrics = new ArrayList<>();
    root.put("metrics", metrics);

    metrics.add(
        bundledMetric(
            "io.opentelemetry.sdk.trace",
            "queueSize",
            MetricInstrument.GAUGE,
            "The number of items queued"));
    metrics.add(
        bundledMetric(
            "io.opentelemetry.sdk.trace",
            "processedSpans",
            MetricInstrument.COUNTER,
            "The number of spans processed by the BatchSpanProcessor. [dropped=true if they were dropped due to high throughput]"));

    metrics.add(
        bundledMetric(
            "io.opentelemetry.sdk.logs",
            "queueSize",
            MetricInstrument.GAUGE,
            "The number of items queued"));
    metrics.add(
        bundledMetric(
            "io.opentelemetry.sdk.logs",
            "processedLogs",
            MetricInstrument.COUNTER,
            "The number of logs processed by the BatchLogRecordProcessor. [dropped=true if they were dropped due to high throughput]"));

    metrics.add(
        bundledMetric(
            "otlp.exporter.exported",
            MetricInstrument.COUNTER,
            "The number of items exported by the otlp exporter."));
    metrics.add(
        bundledMetric(
            "otlp.exporter.seen",
            MetricInstrument.COUNTER,
            "The number of items seen by the otlp exporter."));

    List<Map<String, Object>> instrumentations = new ArrayList<>();
    root.put("instrumentations", instrumentations);

    instrumentations.addAll(parseInstrumentations(otelJavaInstrumentationVersion));

    // splunk instrumentations
    instrumentations.add(
        splunkInstrumentation(
                "jvm-metrics-splunk",
                "This instrumentation enables additional JVM metrics used by profiling dashboards.")
            .component("Java Platform", null)
            .bundledMetric(
                "jvm.memory.allocated",
                MetricInstrument.COUNTER,
                "Approximate sum of heap allocations.")
            .bundledMetric(
                "jvm.gc.pause.count",
                MetricInstrument.COUNTER,
                "Number of gc pauses. This metric will be removed in a future release.")
            .bundledMetric(
                "jvm.gc.pause.totalTime",
                MetricInstrument.COUNTER,
                "Time spent in GC pause. This metric will be removed in a future release.")
            .build());

    instrumentations.add(
        splunkInstrumentation(
                "khttp",
                "This instrumentation enables HTTP client spans and HTTP client metrics for khttp.")
            .component("khttp", "0.1 and higher")
            .httpClientMetrics()
            .addSetting(
                setting(
                    "otel.instrumentation.http.known-methods",
                    "Configures the instrumentation to recognize an alternative set of HTTP request methods. All other methods will be treated as `_OTHER`.",
                    "CONNECT,DELETE,GET,HEAD,OPTIONS,PATCH,POST,PUT,TRACE",
                    SettingType.LIST,
                    SettingCategory.INSTRUMENTATION))
            .addSetting(
                setting(
                    "otel.instrumentation.http.client.capture-request-headers",
                    "List of HTTP request headers to capture in HTTP client telemetry.",
                    "",
                    SettingType.LIST,
                    SettingCategory.INSTRUMENTATION))
            .addSetting(
                setting(
                    "otel.instrumentation.http.client.capture-response-headers",
                    "List of HTTP response headers to capture in HTTP client telemetry.",
                    "",
                    SettingType.LIST,
                    SettingCategory.INSTRUMENTATION))
            .addSetting(
                setting(
                    "otel.instrumentation.common.peer-service-mapping",
                    "Used to specify a mapping from host names or IP addresses to peer services.",
                    "",
                    SettingType.MAP,
                    SettingCategory.INSTRUMENTATION))
            .addSetting(
                setting(
                    "otel.instrumentation.http.client.experimental.redact-query-parameters",
                    "Redact sensitive URL parameters. See https://opentelemetry.io/docs/specs/semconv/http/http-spans.",
                    "true",
                    SettingType.BOOLEAN,
                    SettingCategory.INSTRUMENTATION))
            .build());
    String webengineInstrumentationDesc =
        "Adds `webengine.name` and `webengine.version` attributes to spans.";
    instrumentations.add(
        splunkInstrumentation("glassfish-splunk", webengineInstrumentationDesc)
            .component("GlassFish", "5.0 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("jetty-splunk", webengineInstrumentationDesc)
            .component("Jetty", "9.4 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("liberty-splunk", webengineInstrumentationDesc)
            .component("Liberty", "20.0 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("tomcat-splunk", webengineInstrumentationDesc)
            .component("Tomcat", "7.0 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("tomee-splunk", webengineInstrumentationDesc)
            .component("TomEE", "7.0 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("weblogic-splunk", webengineInstrumentationDesc)
            .component("WebLogic", "12.1 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("websphere-splunk", webengineInstrumentationDesc)
            .component("WebSphere", "8.5.5 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("wildfly-splunk", webengineInstrumentationDesc)
            .component("WildFly", "13.0 and higher")
            .build());

    Collections.sort(instrumentations, Comparator.comparing(i -> Objects.toString(i.get("keys"))));

    List<Map<String, Object>> resourceProviders = new ArrayList<>();
    root.put("resource_detectors", resourceProviders);
    // OpenTelemetry Java Instrumentation Resource Providers
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.ContainerResourceProvider",
                "Container detector.",
                List.of("container.id"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.HostIdResourceProvider",
                "Host id detector.",
                List.of("host.id"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.HostResourceProvider",
                "Host detector.",
                List.of("host.name", "host.arch"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.JarServiceNameDetector",
                "Jar service name detector.",
                List.of("service.name"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.ManifestResourceProvider",
                "Manifest service name and version detector.",
                List.of("service.name", "service.version"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.OsResourceProvider",
                "Os detector.",
                List.of("os.type", "os.description", "os.version"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.ProcessResourceProvider",
                "Process detector.",
                List.of(
                    "process.pid",
                    "process.executable.path",
                    "process.command_args",
                    "process.command_line"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.resources.ProcessRuntimeResourceProvider",
                "Process runtime detector.",
                List.of(
                    "process.runtime.name",
                    "process.runtime.version",
                    "process.runtime.description"))
            .dependency(
                "OpenTelemetry Java Instrumentation Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/resources/library",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    // OpenTelemetry Java Instrumentation Spring Boot Resource Providers
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.spring.resources.SpringBootServiceNameDetector",
                "Spring boot service name detector.",
                List.of("service.name"))
            .dependency(
                "OpenTelemetry Java Instrumentation Spring Boot Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/spring/spring-boot-resources/javaagent",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-spring-boot-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.instrumentation.spring.resources.SpringBootServiceVersionDetector",
                "Spring boot service version detector.",
                List.of("service.version"))
            .dependency(
                "OpenTelemetry Java Instrumentation Spring Boot Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/spring/spring-boot-resources/javaagent",
                "https://central.sonatype.com/artifact/io.opentelemetry.instrumentation/opentelemetry-spring-boot-resources",
                otelJavaInstrumentationAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    // OpenTelemetry Java Contrib Resource Providers
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.resourceproviders.AppServerServiceNameProvider",
                "Application server service name detector.",
                List.of("service.name"))
            .dependency(
                "OpenTelemetry Java Contrib Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/resource-providers",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-resource-providers",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    // OpenTelemetry AWS Resource Providers
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.aws.resource.BeanstalkResourceProvider",
                "Beanstalk detector.",
                List.of(
                    "service.instance.id",
                    "service.version",
                    "service.namespace",
                    "cloud.provider",
                    "cloud.platform"))
            .dependency(
                "OpenTelemetry AWS Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/aws-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-aws-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.aws.resource.Ec2ResourceProvider",
                "Ec2 detector.",
                List.of(
                    "cloud.provider",
                    "cloud.platform",
                    "host.id",
                    "cloud.availability_zone",
                    "host.type",
                    "host.image.id",
                    "cloud.account.id",
                    "cloud.region",
                    "host.name"))
            .dependency(
                "OpenTelemetry AWS Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/aws-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-aws-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.aws.resource.EcsResourceProvider",
                "Ecs detector.",
                List.of(
                    "cloud.provider",
                    "container.id",
                    "container.name",
                    "aws.ecs.container.arn",
                    "container.image.name",
                    "container.image.tag",
                    "aws.ecs.container.image.id",
                    "aws.log.group.names",
                    "aws.log.stream.names",
                    "aws.log.group.arns",
                    "aws.log.stream.arns",
                    "aws.ecs.task.arn",
                    "aws.ecs.launchtype",
                    "aws.ecs.task.family",
                    "aws.ecs.task.revision"))
            .dependency(
                "OpenTelemetry AWS Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/aws-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-aws-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.aws.resource.EksResourceProvider",
                "Eks detector.",
                List.of("cloud.provider", "cloud.platform", "k8s.cluster.name", "container.id"))
            .dependency(
                "OpenTelemetry AWS Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/aws-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-aws-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.aws.resource.LambdaResourceProvider",
                "Lambda detector.",
                List.of(
                    "cloud.provider",
                    "cloud.platform",
                    "cloud.region",
                    "faas.name",
                    "faas.version"))
            .dependency(
                "OpenTelemetry AWS Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/aws-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-aws-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    // OpenTelemetry GCP Resource Providers
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.gcp.resource.GCPResourceProvider",
                "GCP detector.",
                List.of(
                    "cloud.platform",
                    "cloud.provider",
                    "cloud.account.id",
                    "cloud.availability_zone",
                    "cloud.region",
                    "host.id",
                    "host.name",
                    "host.type",
                    "k8s.pod.name",
                    "k8s.namespace.name",
                    "k8s.container.name",
                    "k8s.cluster.name",
                    "faas.name",
                    "faas.version",
                    "faas.instance"))
            .dependency(
                "OpenTelemetry GCP Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/gcp-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-gcp-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    // OpenTelemetry Azure Resource Providers
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.azure.resource.AzureAksResourceProvider",
                "AKS detector.",
                List.of("cloud.platform", "cloud.provider", "k8s.cluster.name"))
            .dependency(
                "OpenTelemetry Azure Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/azure-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-azure-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.azure.resource.AzureAppServiceResourceProvider",
                "App service detector.",
                List.of(
                    "cloud.platform",
                    "cloud.provider",
                    "service.name",
                    "cloud.region",
                    "cloud.resource_id",
                    "deployment.environment.name",
                    "host.id",
                    "service.instance.id",
                    "azure.app.service.stamp"))
            .dependency(
                "OpenTelemetry Azure Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/azure-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-azure-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.azure.resource.AzureContainersResourceProvider",
                "Container detector.",
                List.of(
                    "cloud.platform",
                    "cloud.provider",
                    "service.name",
                    "service.instance.id",
                    "service.version"))
            .dependency(
                "OpenTelemetry Azure Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/azure-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-azure-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.azure.resource.AzureFunctionsResourceProvider",
                "Function detector.",
                List.of(
                    "cloud.platform",
                    "cloud.provider",
                    "faas.max_memory",
                    "cloud.region",
                    "faas.name",
                    "faas.version",
                    "faas.instance"))
            .dependency(
                "OpenTelemetry Azure Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/azure-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-azure-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());
    resourceProviders.add(
        resourceProvider(
                "io.opentelemetry.contrib.azure.resource.AzureVmResourceProvider",
                "Vm detector.",
                List.of(
                    "cloud.platform",
                    "cloud.provider",
                    "cloud.region",
                    "cloud.resource_id",
                    "host.id",
                    "host.name",
                    "host.type",
                    "os.type",
                    "os.version",
                    "azure.vm.scaleset.name",
                    "azure.vm.sku"))
            .dependency(
                "OpenTelemetry Azure Resource Providers",
                "https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/azure-resources",
                "https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-azure-resources",
                otelJavaContribAlphaVersion,
                Stability.EXPERIMENTAL)
            .build());

    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setIndent(2);
    options.setIndicatorIndent(2);
    options.setIndentWithIndicator(true);
    Yaml yaml = new Yaml(options);
    try (Writer writer = new FileWriter(outputPath, StandardCharsets.UTF_8)) {
      yaml.dump(root, writer);
    }
  }

  private static List<Map<String, Object>> parseSdkConfiguration() throws IOException {
    List<Map<String, Object>> settings = new ArrayList<>();
    Yaml yaml = new Yaml();
    Map<String, List<Map<String, Object>>> metadata;
    try (InputStream inputStream =
        MetadataGenerator.class.getResourceAsStream("/sdk-configuration.yaml")) {
      metadata = yaml.load(inputStream);
    }
    for (Map<String, Object> property : metadata.get("properties")) {
      SettingType settingType = SettingType.STRING;
      String name = property.get("system_property").toString();
      String defaultValue = property.get("default").toString();
      if ("true".equals(defaultValue) || "false".equals(defaultValue)) {
        settingType = SettingType.BOOLEAN;
      } else if (name.endsWith("timeout")
          || name.endsWith("limit")
          || name.endsWith("delay")
          || name.endsWith("size")
          || name.endsWith("interval")
          || name.endsWith("port")) {
        settingType = SettingType.INT;
      }
      SettingCategory category;
      switch (property.get("category").toString()) {
        case "exporter":
          category = SettingCategory.EXPORTER;
          break;
        case "general":
          category = SettingCategory.GENERAL;
          break;
        case "resource_provider":
          category = SettingCategory.RESOURCE_PROVIDER;
          break;
        case "instrumentation":
          category = SettingCategory.INSTRUMENTATION;
          break;
        default:
          throw new IllegalStateException("unexpected category: " + property.get("category"));
      }
      settings.add(
          setting(
              name, property.get("description").toString(), defaultValue, settingType, category));
    }
    return settings;
  }

  private static List<Map<String, Object>> parseInstrumentations(
      String otelJavaInstrumentationVersion) throws IOException {
    String url;
    if (otelJavaInstrumentationVersion.endsWith("-SNAPSHOT")) {
      url =
          "https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-instrumentation/HEAD/docs/instrumentation-list.yaml";
    } else {
      url =
          "https://raw.githubusercontent.com/open-telemetry/opentelemetry-java-instrumentation/refs/tags/v"
              + otelJavaInstrumentationVersion
              + "/docs/instrumentation-list.yaml";
    }
    return parseInstrumentations(new URL(url));
  }

  private static List<Map<String, Object>> parseInstrumentations(URL url) throws IOException {
    Yaml yaml = new Yaml();
    Map<String, Object> metadata;
    try (InputStream inputStream = url.openStream()) {
      metadata = yaml.load(inputStream);
    }

    if (!"0.5".equals(metadata.get("file_format").toString())) {
      throw new IllegalStateException(
          "unexpected file format version: " + metadata.get("file_format"));
    }
    List<Map<String, Object>> result = new ArrayList<>();

    handle(result, (List<Map<String, Object>>) metadata.get("libraries"));
    handle(result, (List<Map<String, Object>>) metadata.get("custom"));

    return result;
  }

  private static void handle(
      List<Map<String, Object>> instrumentations, List<Map<String, Object>> infos) {
    for (Map<String, Object> info : infos) {
      // only javaagent instrumentations
      if (Boolean.TRUE.equals(info.get("has_javaagent"))) {
        String name = info.get("name").toString();
        String description = info.get("description").toString();
        boolean disabledByDefault = Boolean.TRUE.equals(info.get("disabled_by_default"));
        // spring batch is disabled by default in upstream but enabled in our distro
        if (disabledByDefault && !name.startsWith("spring-batch")) {
          if (!description.isEmpty()) {
            description = description.trim() + "\n";
          }
          description += "This instrumentation is disabled by default.";
        }
        InstrumentationBuilder builder = instrumentation(name, description);
        String displayName = Objects.toString(info.get("display_name"), null);
        if (displayName != null) {
          builder.component(
              displayName, Objects.toString(info.get("javaagent_target_versions"), null));
        }

        List<Map<String, Object>> configurations =
            (List<Map<String, Object>>) info.get("configurations");
        if (configurations != null) {
          for (Map<String, Object> configuration : configurations) {
            builder.addSetting(
                setting(
                    configuration.get("name").toString(),
                    configuration.get("description").toString(),
                    configuration.get("default").toString(),
                    toSettingType(configuration.get("type").toString()),
                    MetadataGenerator.SettingCategory.INSTRUMENTATION));
          }
        }

        List<Map<String, Object>> telemetry = (List<Map<String, Object>>) info.get("telemetry");
        if (telemetry != null) {
          // sort default configuration first
          telemetry.sort(Comparator.comparingInt(c -> "default".equals(c.get("when")) ? -1 : 0));

          boolean defaultSeen = false;
          Set<String> seenMetrics = new HashSet<>();
          for (Map<String, Object> telemetryConfiguration : telemetry) {
            String configuration = telemetryConfiguration.get("when").toString();
            boolean isDefault = "default".equals(configuration);
            List<Map<String, Object>> metrics =
                (List<Map<String, Object>>) telemetryConfiguration.get("metrics");

            if (isDefault) {
              defaultSeen = true;
            } else if (!defaultSeen && metrics != null && !metrics.isEmpty()) {
              // skip when default configuration was not present
              // spring-webflux-5.0 doesn't have default configuration, use the controller telemetry
              // configuration
              if ("otel.instrumentation.common.experimental.controller-telemetry.enabled"
                  .equals(configuration)) {
                isDefault = true;
              } else {
                continue;
              }
            } else if (!configuration.contains("=")) {
              // only include configurations that look like setting names, this excludes Java17
              // configuration for runtime-telemetry
              continue;
            }

            if (metrics != null) {
              for (Map<String, Object> metric : metrics) {
                String metricName = metric.get("name").toString();
                String metricDescription = metric.get("description").toString();
                if (isDefault) {
                  seenMetrics.add(metricName);
                } else if (seenMetrics.contains(metricName)) {
                  // skip over metrics that we have already added
                  continue;
                } else {
                  seenMetrics.add(metricName);
                  // metric is not present in default configuration
                  if (!metricDescription.isEmpty()) {
                    metricDescription = metricDescription.trim() + "\n";
                  }
                  metricDescription +=
                      "This metric is disabled by default and can be enabled with `"
                          + configuration
                          + "`.";
                }

                builder.metric(
                    metricName,
                    toMetricInstrument(metric.get("instrument").toString()),
                    metricDescription,
                    bundledMetrics.contains(metricName) ? BUNDLED_METRIC : CUSTOM_METRIC);
              }
            }
          }
        }

        instrumentations.add(builder.build());
      }
    }
  }

  private static Map<String, Object> dependency(
      String name, String source, String version, Stability stability) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", name);
    map.put("source_href", source);
    map.put("version", version);
    map.put("stability", stability.value());

    return map;
  }

  private static Map<String, Object> setting(
      String property,
      String description,
      String defaultValue,
      SettingType type,
      SettingCategory category) {
    if ("otel.instrumentation.spring-batch.item.enabled".equals(property)) {
      // SplunkConfiguration overrides the default value of this settings to true
      defaultValue = "true";
    }

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("property", property);
    map.put("env", toEnvVar(property));
    map.put("description", description);
    map.put("default", defaultValue);
    map.put("type", type.value());
    map.put("category", category.value());

    return map;
  }

  private static Map<String, Object> bundledMetric(
      String metricName, MetricInstrument instrument, String description) {
    return bundledMetric(null, metricName, instrument, description);
  }

  private static Map<String, Object> bundledMetric(
      String instrumentationScopeName,
      String metricName,
      MetricInstrument instrument,
      String description) {
    return metric(instrumentationScopeName, metricName, instrument, description, BUNDLED_METRIC);
  }

  private static Map<String, Object> metric(
      String instrumentationScopeName,
      String metricName,
      MetricInstrument instrument,
      String description,
      String categoryNotes) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (instrumentationScopeName != null) {
      map.put("instrumentation_scope_name", instrumentationScopeName);
    }
    map.put("metric_name", metricName);
    map.put("instrument", instrument.value());
    map.put("description", description);
    map.put("category_notes", categoryNotes);

    return map;
  }

  static String toEnvVar(String systemProperty) {
    return systemProperty.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
  }

  static MetadataGenerator.SettingType toSettingType(String value) {
    switch (value) {
      case "boolean":
        return MetadataGenerator.SettingType.BOOLEAN;
      case "int":
        return MetadataGenerator.SettingType.INT;
      case "string":
        return MetadataGenerator.SettingType.STRING;
      case "list":
        return MetadataGenerator.SettingType.LIST;
      case "map":
        return MetadataGenerator.SettingType.MAP;
      default:
        throw new IllegalArgumentException("Unsupported setting type: " + value);
    }
  }

  static MetadataGenerator.MetricInstrument toMetricInstrument(String value) {
    switch (value) {
      case "counter":
        return MetadataGenerator.MetricInstrument.COUNTER;
      case "gauge":
        return MetadataGenerator.MetricInstrument.GAUGE;
      case "histogram":
        return MetadataGenerator.MetricInstrument.HISTOGRAM;
      case "updowncounter":
        return MetadataGenerator.MetricInstrument.UP_DOWN_COUNTER;
      default:
        throw new IllegalArgumentException("Unsupported metric instrument: " + value);
    }
  }

  static InstrumentationBuilder instrumentation(String key, String description) {
    return instrumentation(key, description, Stability.EXPERIMENTAL, Support.COMMUNITY);
  }

  static InstrumentationBuilder instrumentation(
      String key, String description, Stability stability, Support support) {
    return instrumentation(Collections.singletonList(key), description, stability, support);
  }

  static InstrumentationBuilder instrumentation(
      List<String> keys, String description, Stability stability, Support support) {
    return new InstrumentationBuilder(keys, description, stability, support);
  }

  static InstrumentationBuilder splunkInstrumentation(String key, String description) {
    return instrumentation(key, description, Stability.EXPERIMENTAL, Support.SUPPORTED);
  }

  private static class InstrumentationBuilder {
    private final List<String> keys;
    private final String description;
    private final Stability stability;
    private final Support support;
    private final List<Object> instrumentedComponents = new ArrayList<>();
    private final List<Map<String, Object>> settings = new ArrayList<>();
    private final List<Object> metrics = new ArrayList<>();

    InstrumentationBuilder(
        List<String> keys, String description, Stability stability, Support support) {
      this.keys = keys;
      this.description = description;
      this.stability = stability;
      this.support = support;
    }

    InstrumentationBuilder component(String name, String supportedVersions) {
      Map<String, Object> map = new LinkedHashMap<>();
      instrumentedComponents.add(map);
      map.put("name", name);
      if (supportedVersions != null) {
        map.put("supported_versions", supportedVersions);
      }

      return this;
    }

    InstrumentationBuilder addSetting(Map<String, Object> setting) {
      settings.add(setting);

      return this;
    }

    InstrumentationBuilder bundledMetric(
        String name, MetricInstrument instrument, String description) {
      return metric(name, instrument, description, BUNDLED_METRIC);
    }

    InstrumentationBuilder customMetric(
        String name, MetricInstrument instrument, String description) {
      return metric(name, instrument, description, CUSTOM_METRIC);
    }

    InstrumentationBuilder metric(
        String name, MetricInstrument instrument, String description, String categoryNotes) {
      Map<String, Object> map = new LinkedHashMap<>();
      metrics.add(map);
      map.put("metric_name", name);
      map.put("instrument", instrument.value());
      map.put("description", description);
      map.put("category_notes", categoryNotes);

      return this;
    }

    InstrumentationBuilder httpClientMetrics() {
      bundledMetric(
          "http.client.request.duration",
          MetricInstrument.HISTOGRAM,
          "Duration of HTTP client requests.");
      customMetric(
          "http.client.request.body.size",
          MetricInstrument.HISTOGRAM,
          "Size of HTTP client request bodies (disabled by default).");
      customMetric(
          "http.client.response.body.size",
          MetricInstrument.HISTOGRAM,
          "Size of HTTP client response bodies (disabled by default).");

      return this;
    }

    Map<String, Object> build() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("keys", keys);
      map.put("instrumented_components", instrumentedComponents);
      if (description != null) {
        map.put("description", description);
      }
      map.put("stability", stability.value());
      map.put("support", support.value());

      List<Object> signals = new ArrayList<>();
      if (!metrics.isEmpty()) {
        signals.add(Collections.singletonMap("metrics", metrics));
      }
      if (!signals.isEmpty()) {
        map.put("signals", signals);
      }
      if (!settings.isEmpty()) {
        map.put("settings", settings);
      }

      return map;
    }
  }

  static ResourceProviderBuilder resourceProvider(
      String key, String description, List<String> attributes) {
    return resourceProvider(
        key, description, attributes, Stability.EXPERIMENTAL, Support.COMMUNITY);
  }

  static ResourceProviderBuilder resourceProvider(
      String key,
      String description,
      List<String> attributes,
      Stability stability,
      Support support) {
    return new ResourceProviderBuilder(key, description, attributes, stability, support);
  }

  private static class ResourceProviderBuilder {
    private final String key;
    private final String description;
    private final List<Object> attributes = new ArrayList<>();
    private final Stability stability;
    private final Support support;
    private final List<Object> dependencies = new ArrayList<>();

    ResourceProviderBuilder(
        String key,
        String description,
        List<String> attributeNames,
        Stability stability,
        Support support) {
      this.key = key;
      this.description = description;
      this.stability = stability;
      this.support = support;
      for (String attributeName : attributeNames) {
        attributes.add(Collections.singletonMap("id", attributeName));
      }
    }

    ResourceProviderBuilder dependency(
        String name, String sourceUrl, String packageUrl, String version, Stability stability) {
      Map<String, Object> map = new LinkedHashMap<>();
      dependencies.add(map);
      map.put("name", name);
      map.put("source_href", sourceUrl);
      if (packageUrl != null) {
        map.put("package_href", packageUrl);
      }
      map.put("version", version);
      map.put("stability", stability.value());

      return this;
    }

    Map<String, Object> build() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("key", key);
      if (description != null) {
        map.put("description", description);
      }
      if (!attributes.isEmpty()) {
        map.put("attributes", attributes);
      }
      map.put("stability", stability.value());
      map.put("support", support.value());
      if (!dependencies.isEmpty()) {
        map.put("dependencies", dependencies);
      }

      return map;
    }
  }

  enum Stability {
    STABLE,
    EXPERIMENTAL;

    String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  enum Support {
    SUPPORTED,
    COMMUNITY;

    String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  enum SettingType {
    BOOLEAN,
    INT,
    STRING,
    LIST,
    MAP;

    String value() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  enum SettingCategory {
    EXPORTER,
    GENERAL,
    INSTRUMENTATION,
    PROFILER,
    RESOURCE_PROVIDER("resource provider");

    private final String value;

    SettingCategory() {
      value = name().toLowerCase(Locale.ROOT);
    }

    SettingCategory(String value) {
      this.value = value;
    }

    String value() {
      return value;
    }
  }

  enum MetricInstrument {
    COUNTER,
    GAUGE,
    HISTOGRAM,
    UP_DOWN_COUNTER;

    String value() {
      return name().toLowerCase(Locale.ROOT).replace("_", "");
    }
  }
}
