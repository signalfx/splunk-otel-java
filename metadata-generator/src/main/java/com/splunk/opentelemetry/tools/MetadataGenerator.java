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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class MetadataGenerator {
  private static final String BUNDLED_METRIC =
      "APM bundled, if data points for the metric contain `telemetry.sdk.language` attribute.";
  private static final String CUSTOM_METRIC = "Custom metric.";

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

    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md

    // Disabling OpenTelemetrySdk
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#disabling-opentelemetrysdk
    /*
    otel.sdk.disabled	OTEL_SDK_DISABLED	If true, disable the OpenTelemetry SDK. Defaults to false.
     */

    settings.add(
        setting(
            "otel.sdk.disabled",
            "If true, disable the OpenTelemetry SDK. Defaults to false.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.GENERAL));

    // Exporters
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#exporters
    /*
    otel.traces.exporter	OTEL_TRACES_EXPORTER	List of exporters to be used for tracing, separated by commas. Default is otlp. none means no autoconfigured exporter.
    otel.metrics.exporter	OTEL_METRICS_EXPORTER	List of exporters to be used for metrics, separated by commas. Default is otlp. none means no autoconfigured exporter.
    otel.logs.exporter	OTEL_LOGS_EXPORTER	List of exporters to be used for logging, separated by commas. Default is otlp. none means no autoconfigured exporter.
    otel.java.experimental.exporter.memory_mode	OTEL_JAVA_EXPERIMENTAL_EXPORTER_MEMORY_MODE	If reusable_data, enable reusable memory mode (on exporters which support it) to reduce allocations. Default is immutable_data. This option is experimental and subject to change or removal.[1]
     */

    settings.add(
        setting(
            "otel.traces.exporter",
            "List of exporters to be used for tracing, separated by commas. Default is otlp. none means no autoconfigured exporter.",
            "otlp",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.metrics.exporter",
            "List of exporters to be used for tracing, separated by commas. Default is otlp. none means no autoconfigured exporter.",
            "otlp",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.logs.exporter",
            "List of exporters to be used for tracing, separated by commas. Default is otlp. none means no autoconfigured exporter.",
            "otlp",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.java.experimental.exporter.memory_mode",
            "If `reusable_data`, enable reusable memory mode (on exporters which support it) to reduce allocations. Default is `immutable_data`. "
                + "This option is experimental and subject to change or removal.",
            "immutable_data",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    // OTLP exporter (span, metric, and log exporters)
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#otlp-exporter-span-metric-and-log-exporters

    /*
    otel.exporter.otlp.endpoint	OTEL_EXPORTER_OTLP_ENDPOINT	The OTLP traces, metrics, and logs endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. If protocol is http/protobuf the version and signal will be appended to the path (e.g. v1/traces, v1/metrics, or v1/logs). Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/{signal} when protocol is http/protobuf.
    otel.exporter.otlp.traces.endpoint	OTEL_EXPORTER_OTLP_TRACES_ENDPOINT	The OTLP traces endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/traces when protocol is http/protobuf.
    otel.exporter.otlp.metrics.endpoint	OTEL_EXPORTER_OTLP_METRICS_ENDPOINT	The OTLP metrics endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/metrics when protocol is http/protobuf.
    otel.exporter.otlp.logs.endpoint	OTEL_EXPORTER_OTLP_LOGS_ENDPOINT	The OTLP logs endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/logs when protocol is http/protobuf.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.endpoint",
            "The OTLP traces, metrics, and logs endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. If protocol is http/protobuf the version and signal will be appended to the path (e.g. v1/traces, v1/metrics, or v1/logs). Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/{signal} when protocol is http/protobuf.",
            "http://localhost:4318",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.endpoint",
            "The OTLP traces endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/traces when protocol is http/protobuf.",
            "http://localhost:4318/v1/traces",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.endpoint",
            "The OTLP metrics endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/metrics when protocol is http/protobuf.",
            "http://localhost:4318/v1/metrics",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.endpoint",
            "The OTLP logs endpoint to connect to. Must be a URL with a scheme of either http or https based on the use of TLS. Default is http://localhost:4317 when protocol is grpc, and http://localhost:4318/v1/logs when protocol is http/protobuf.",
            "http://localhost:4318/v1/logs",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.certificate	OTEL_EXPORTER_OTLP_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP trace, metric, or log server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.
    otel.exporter.otlp.traces.certificate	OTEL_EXPORTER_OTLP_TRACES_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP trace server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.
    otel.exporter.otlp.metrics.certificate	OTEL_EXPORTER_OTLP_METRICS_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP metric server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.
    otel.exporter.otlp.logs.certificate	OTEL_EXPORTER_OTLP_LOGS_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP log server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP trace, metric, or log server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP trace server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP metric server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP log server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default the host platform's trusted root certificates are used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.client.key	OTEL_EXPORTER_OTLP_CLIENT_KEY	The path to the file containing private client key to use when verifying an OTLP trace, metric, or log client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key is used.
    otel.exporter.otlp.traces.client.key	OTEL_EXPORTER_OTLP_TRACES_CLIENT_KEY	The path to the file containing private client key to use when verifying an OTLP trace client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key file is used.
    otel.exporter.otlp.metrics.client.key	OTEL_EXPORTER_OTLP_METRICS_CLIENT_KEY	The path to the file containing private client key to use when verifying an OTLP metric client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key file is used.
    otel.exporter.otlp.logs.client.key	OTEL_EXPORTER_OTLP_LOGS_CLIENT_KEY	The path to the file containing private client key to use when verifying an OTLP log client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key file is used.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.client.key",
            "The path to the file containing private client key to use when verifying an OTLP trace, metric, or log client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.client.key",
            "The path to the file containing private client key to use when verifying an OTLP trace client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key file is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.client.key",
            "The path to the file containing private client key to use when verifying an OTLP metric client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key file is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.client.key",
            "The path to the file containing private client key to use when verifying an OTLP log client's TLS credentials. The file should contain one private key PKCS8 PEM format. By default no client key file is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.client.certificate	OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP trace, metric, or log client's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.
    otel.exporter.otlp.traces.client.certificate	OTEL_EXPORTER_OTLP_TRACES_CLIENT_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP trace server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.
    otel.exporter.otlp.metrics.client.certificate	OTEL_EXPORTER_OTLP_METRICS_CLIENT_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP metric server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.
    otel.exporter.otlp.logs.client.certificate	OTEL_EXPORTER_OTLP_LOGS_CLIENT_CERTIFICATE	The path to the file containing trusted certificates to use when verifying an OTLP log server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.client.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP trace, metric, or log client's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.client.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP trace server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.client.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP metric server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.client.certificate",
            "The path to the file containing trusted certificates to use when verifying an OTLP log server's TLS credentials. The file should contain one or more X.509 certificates in PEM format. By default no chain file is used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.headers	OTEL_EXPORTER_OTLP_HEADERS	Key-value pairs separated by commas to pass as request headers on OTLP trace, metric, and log requests.
    otel.exporter.otlp.traces.headers	OTEL_EXPORTER_OTLP_TRACES_HEADERS	Key-value pairs separated by commas to pass as request headers on OTLP trace requests.
    otel.exporter.otlp.metrics.headers	OTEL_EXPORTER_OTLP_METRICS_HEADERS	Key-value pairs separated by commas to pass as request headers on OTLP metrics requests.
    otel.exporter.otlp.logs.headers	OTEL_EXPORTER_OTLP_LOGS_HEADERS	Key-value pairs separated by commas to pass as request headers on OTLP logs requests.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.headers",
            "Key-value pairs separated by commas to pass as request headers on OTLP trace, metric, and log requests.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.headers",
            "Key-value pairs separated by commas to pass as request headers on OTLP trace requests.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.headers",
            "Key-value pairs separated by commas to pass as request headers on OTLP metrics requests.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.headers",
            "Key-value pairs separated by commas to pass as request headers on OTLP logs requests.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.compression	OTEL_EXPORTER_OTLP_COMPRESSION	The compression type to use on OTLP trace, metric, and log requests. Options include gzip. By default no compression will be used.
    otel.exporter.otlp.traces.compression	OTEL_EXPORTER_OTLP_TRACES_COMPRESSION	The compression type to use on OTLP trace requests. Options include gzip. By default no compression will be used.
    otel.exporter.otlp.metrics.compression	OTEL_EXPORTER_OTLP_METRICS_COMPRESSION	The compression type to use on OTLP metric requests. Options include gzip. By default no compression will be used.
    otel.exporter.otlp.logs.compression	OTEL_EXPORTER_OTLP_LOGS_COMPRESSION	The compression type to use on OTLP log requests. Options include gzip. By default no compression will be used.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.compression",
            "The compression type to use on OTLP trace, metric, and log requests. Options include gzip. By default no compression will be used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.compression",
            "The compression type to use on OTLP trace requests. Options include gzip. By default no compression will be used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.compression",
            "The compression type to use on OTLP metric requests. Options include gzip. By default no compression will be used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.compression",
            "The compression type to use on OTLP log requests. Options include gzip. By default no compression will be used.",
            "",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.timeout	OTEL_EXPORTER_OTLP_TIMEOUT	The maximum waiting time, in milliseconds, allowed to send each OTLP trace, metric, and log batch. Default is 10000.
    otel.exporter.otlp.traces.timeout	OTEL_EXPORTER_OTLP_TRACES_TIMEOUT	The maximum waiting time, in milliseconds, allowed to send each OTLP trace batch. Default is 10000.
    otel.exporter.otlp.metrics.timeout	OTEL_EXPORTER_OTLP_METRICS_TIMEOUT	The maximum waiting time, in milliseconds, allowed to send each OTLP metric batch. Default is 10000.
    otel.exporter.otlp.logs.timeout	OTEL_EXPORTER_OTLP_LOGS_TIMEOUT	The maximum waiting time, in milliseconds, allowed to send each OTLP log batch. Default is 10000.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.timeout",
            "The maximum waiting time, in milliseconds, allowed to send each OTLP trace, metric, and log batch. Default is 10000.",
            "10000",
            SettingType.INT,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.timeout",
            "The maximum waiting time, in milliseconds, allowed to send each OTLP trace batch. Default is 10000.",
            "10000",
            SettingType.INT,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.timeout",
            "The maximum waiting time, in milliseconds, allowed to send each OTLP metric batch. Default is 10000.",
            "10000",
            SettingType.INT,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.timeout",
            "The maximum waiting time, in milliseconds, allowed to send each OTLP log batch. Default is 10000.",
            "10000",
            SettingType.INT,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.protocol	OTEL_EXPORTER_OTLP_PROTOCOL	The transport protocol to use on OTLP trace, metric, and log requests. Options include grpc and http/protobuf. Default is grpc.
    otel.exporter.otlp.traces.protocol	OTEL_EXPORTER_OTLP_TRACES_PROTOCOL	The transport protocol to use on OTLP trace requests. Options include grpc and http/protobuf. Default is grpc.
    otel.exporter.otlp.metrics.protocol	OTEL_EXPORTER_OTLP_METRICS_PROTOCOL	The transport protocol to use on OTLP metric requests. Options include grpc and http/protobuf. Default is grpc.
    otel.exporter.otlp.logs.protocol	OTEL_EXPORTER_OTLP_LOGS_PROTOCOL	The transport protocol to use on OTLP log requests. Options include grpc and http/protobuf. Default is grpc.
    */
    settings.add(
        setting(
            "otel.exporter.otlp.protocol",
            "The transport protocol to use on OTLP trace, metric, and log requests. Options include grpc and http/protobuf.",
            "http/protobuf",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.traces.protocol",
            "The transport protocol to use on OTLP trace requests. Options include grpc and http/protobuf.",
            "http/protobuf",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.protocol",
            "The transport protocol to use on OTLP metric requests. Options include grpc and http/protobuf.",
            "http/protobuf",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.logs.protocol",
            "The transport protocol to use on OTLP log requests. Options include grpc and http/protobuf.",
            "http/protobuf",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.exporter.otlp.metrics.temporality.preference	OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE	The preferred output aggregation temporality. Options include DELTA, LOWMEMORY, and CUMULATIVE. If CUMULATIVE, all instruments will have cumulative temporality. If DELTA, counter (sync and async) and histograms will be delta, up down counters (sync and async) will be cumulative. If LOWMEMORY, sync counter and histograms will be delta, async counter and up down counters (sync and async) will be cumulative. Default is CUMULATIVE.
    otel.exporter.otlp.metrics.default.histogram.aggregation	OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION	The preferred default histogram aggregation. Options include BASE2_EXPONENTIAL_BUCKET_HISTOGRAM and EXPLICIT_BUCKET_HISTOGRAM. Default is EXPLICIT_BUCKET_HISTOGRAM.
     */
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.temporality.preference",
            "The preferred output aggregation temporality. Options include DELTA, LOWMEMORY, and CUMULATIVE. If CUMULATIVE, all instruments will have cumulative temporality. If DELTA, counter (sync and async) and histograms will be delta, up down counters (sync and async) will be cumulative. If LOWMEMORY, sync counter and histograms will be delta, async counter and up down counters (sync and async) will be cumulative. Default is CUMULATIVE.",
            "CUMULATIVE",
            SettingType.STRING,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.otlp.metrics.default.histogram.aggregation",
            "The preferred default histogram aggregation. Options include BASE2_EXPONENTIAL_BUCKET_HISTOGRAM and EXPLICIT_BUCKET_HISTOGRAM. Default is EXPLICIT_BUCKET_HISTOGRAM.",
            "EXPLICIT_BUCKET_HISTOGRAM",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    /*
    otel.experimental.exporter.otlp.retry.enabled	OTEL_EXPERIMENTAL_EXPORTER_OTLP_RETRY_ENABLED	If true, enable experimental retry support. Default is false.
     */
    settings.add(
        setting(
            "otel.experimental.exporter.otlp.retry.enabled",
            "If true, enable experimental retry support. Default is false.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.EXPORTER));

    // OpenTelemetry Resource
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#opentelemetry-resource

    /*
    otel.resource.attributes	OTEL_RESOURCE_ATTRIBUTES	Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3
    otel.service.name	OTEL_SERVICE_NAME	Specify logical service name. Takes precedence over service.name defined with otel.resource.attributes
    otel.experimental.resource.disabled-keys	OTEL_EXPERIMENTAL_RESOURCE_DISABLED_KEYS	Specify resource attribute keys that are filtered.
     */

    settings.add(
        setting(
            "otel.resource.attributes",
            "Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.service.name",
            "Specify logical service name. Takes precedence over service.name defined with otel.resource.attributes",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.experimental.resource.disabled-keys",
            "Specify resource attribute keys that are filtered.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // Disabling Automatic ResourceProviders
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#disabling-automatic-resourceproviders

    /*
    otel.java.enabled.resource.providers	OTEL_JAVA_ENABLED_RESOURCE_PROVIDERS	Enables one or more ResourceProvider types. If unset, all resource providers are enabled.
    otel.java.disabled.resource.providers	OTEL_JAVA_DISABLED_RESOURCE_PROVIDERS	Disables one or more ResourceProvider types
     */

    settings.add(
        setting(
            "otel.java.enabled.resource.providers",
            "Enables one or more ResourceProvider types. If unset, all resource providers are enabled.",
            "",
            SettingType.STRING,
            SettingCategory.RESOURCE_PROVIDER));
    settings.add(
        setting(
            "otel.java.disabled.resource.providers",
            "Disables one or more ResourceProvider types.",
            "",
            SettingType.STRING,
            SettingCategory.RESOURCE_PROVIDER));

    // Attribute limits
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#attribute-limits

    /*
    otel.attribute.value.length.limit	OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT	The maximum length of attribute values. Applies to spans and logs. By default there is no limit.
    otel.attribute.count.limit	OTEL_ATTRIBUTE_COUNT_LIMIT	The maximum number of attributes. Applies to spans, span events, span links, and logs. Default is 128.
     */

    settings.add(
        setting(
            "otel.attribute.value.length.limit",
            "The maximum length of attribute values. Applies to spans and logs. By default there is no limit.",
            "",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.attribute.count.limit",
            "The maximum number of attributes. Applies to spans, span events, span links, and logs. Default is 128.",
            "128",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));

    // Propagator
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#propagator

    /*
    otel.propagators	OTEL_PROPAGATORS	The propagators to be used. Use a comma-separated list for multiple propagators. Default is tracecontext,baggage (W3C).
     */

    settings.add(
        setting(
            "otel.propagators",
            "The propagators to be used. Use a comma-separated list for multiple propagators. Default is tracecontext,baggage (W3C).",
            "tracecontext,baggage",
            SettingType.STRING,
            SettingCategory.GENERAL));

    // Zipkin exporter
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#zipkin-exporter

    /*
    otel.exporter.zipkin.endpoint	OTEL_EXPORTER_ZIPKIN_ENDPOINT	The Zipkin endpoint to connect to. Default is http://localhost:9411/api/v2/spans. Currently only HTTP is supported.
     */

    settings.add(
        setting(
            "otel.exporter.zipkin.endpoint",
            "The Zipkin endpoint to connect to. Default is http://localhost:9411/api/v2/spans. Currently only HTTP is supported.",
            "http://localhost:9411/api/v2/spans",
            SettingType.STRING,
            SettingCategory.EXPORTER));

    // Batch span processor
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#batch-span-processor

    /*
    otel.bsp.schedule.delay	OTEL_BSP_SCHEDULE_DELAY	The interval, in milliseconds, between two consecutive exports. Default is 5000.
    otel.bsp.max.queue.size	OTEL_BSP_MAX_QUEUE_SIZE	The maximum queue size. Default is 2048.
    otel.bsp.max.export.batch.size	OTEL_BSP_MAX_EXPORT_BATCH_SIZE	The maximum batch size. Default is 512.
    otel.bsp.export.timeout	OTEL_BSP_EXPORT_TIMEOUT	The maximum allowed time, in milliseconds, to export data. Default is 30000.
     */

    settings.add(
        setting(
            "otel.bsp.schedule.delay",
            "The interval, in milliseconds, between two consecutive exports. Default is 5000.",
            "5000",
            SettingType.INT,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.bsp.max.queue.size",
            "The maximum queue size. Default is 2048.",
            "2048",
            SettingType.INT,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.bsp.max.export.batch.size",
            "The maximum batch size. Default is 512.",
            "512",
            SettingType.INT,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.bsp.export.timeout",
            "The maximum allowed time, in milliseconds, to export data. Default is 30000.",
            "30000",
            SettingType.INT,
            SettingCategory.GENERAL));

    // Sampler
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#sampler

    /*
    otel.traces.sampler	OTEL_TRACES_SAMPLER	The sampler to use for tracing. Defaults to parentbased_always_on
    otel.traces.sampler.arg	OTEL_TRACES_SAMPLER_ARG	An argument to the configured tracer if supported, for example a ratio.
     */

    settings.add(
        setting(
            "otel.traces.sampler",
            "The sampler to use for tracing. Defaults to parentbased_always_on",
            "always_on",
            SettingType.STRING,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.traces.sampler.arg",
            "An argument to the configured tracer if supported, for example a ratio.",
            "",
            SettingType.STRING,
            SettingCategory.GENERAL));

    // Span limits
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#span-limits

    /*
    otel.span.attribute.value.length.limit	OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT	The maximum length of span attribute values. Takes precedence over otel.attribute.value.length.limit. By default there is no limit.
    otel.span.attribute.count.limit	OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT	The maximum number of attributes per span. Takes precedence over otel.attribute.count.limit. Default is 128.
    otel.span.event.count.limit	OTEL_SPAN_EVENT_COUNT_LIMIT	The maximum number of events per span. Default is 128.
    otel.span.link.count.limit	OTEL_SPAN_LINK_COUNT_LIMIT	The maximum number of links per span. Default is 128
     */

    settings.add(
        setting(
            "otel.span.attribute.value.length.limit",
            "The maximum length of span attribute values. Takes precedence over otel.attribute.value.length.limit. By default there is no limit.",
            "",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.span.attribute.count.limit",
            "The maximum number of attributes per span. Takes precedence over otel.attribute.count.limit. Default is 128.",
            "128",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.span.event.count.limit",
            "The maximum number of events per span. Default is 128.",
            "128",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.span.link.count.limit",
            "TThe maximum number of links per span. Default is 128.",
            "128",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));

    // Metrics
    // https://opentelemetry.io/docs/languages/java/configuration/#properties-metrics

    settings.add(
        setting(
            "otel.metric.export.interval",
            "The interval, in milliseconds, between the start of two export attempts. Default is 60000.",
            "60000",
            SettingType.INT,
            SettingCategory.EXPORTER));

    settings.add(
        setting(
            "otel.metrics.exemplar.filter",
            "The filter for exemplar sampling. Can be ALWAYS_OFF, ALWAYS_ON or TRACE_BASED. Default is TRACE_BASED.",
            "TRACE_BASED",
            SettingType.STRING,
            SettingCategory.GENERAL));

    settings.add(
        setting(
            "otel.java.metrics.cardinality.limit",
            "If set, configure cardinality limit. The value dictates the maximum number of distinct points per metric. Default is 2000.",
            "2000",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));

    // Prometheus exporter
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#prometheus-exporter

    /*
    otel.exporter.prometheus.port	OTEL_EXPORTER_PROMETHEUS_PORT	The local port used to bind the prometheus metric server. Default is 9464.
    otel.exporter.prometheus.host	OTEL_EXPORTER_PROMETHEUS_HOST	The local address used to bind the prometheus metric server. Default is 0.0.0.0.
     */

    settings.add(
        setting(
            "otel.exporter.prometheus.port",
            "The local port used to bind the prometheus metric server. Default is 9464.",
            "9464",
            SettingType.INT,
            SettingCategory.EXPORTER));
    settings.add(
        setting(
            "otel.exporter.prometheus.host",
            "The local address used to bind the prometheus metric server. Default is 0.0.0.0.",
            "0.0.0.0",
            SettingType.INT,
            SettingCategory.EXPORTER));

    // Batch log record processor
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#batch-log-record-processor

    /*
    otel.blrp.schedule.delay	OTEL_BLRP_SCHEDULE_DELAY	The interval, in milliseconds, between two consecutive exports. Default is 1000.
    otel.blrp.max.queue.size	OTEL_BLRP_MAX_QUEUE_SIZE	The maximum queue size. Default is 2048.
    otel.blrp.max.export.batch.size	OTEL_BLRP_MAX_EXPORT_BATCH_SIZE	The maximum batch size. Default is 512.
    otel.blrp.export.timeout	OTEL_BLRP_EXPORT_TIMEOUT	The maximum allowed time, in milliseconds, to export data. Default is 30000.
     */

    settings.add(
        setting(
            "otel.blrp.schedule.delay",
            "The interval, in milliseconds, between two consecutive exports. Default is 1000.",
            "1000",
            SettingType.INT,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.blrp.max.queue.size",
            "The maximum queue size. Default is 2048.",
            "2048",
            SettingType.INT,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.blrp.max.export.batch.size",
            "The maximum batch size. Default is 512.",
            "512",
            SettingType.INT,
            SettingCategory.GENERAL));
    settings.add(
        setting(
            "otel.blrp.export.timeout",
            "The maximum allowed time, in milliseconds, to export data. Default is 30000.",
            "30000",
            SettingType.INT,
            SettingCategory.GENERAL));

    // File Configuration
    // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#file-configuration

    /*
    otel.experimental.config.file	OTEL_EXPERIMENTAL_CONFIG_FILE	The path to the SDK configuration file. Defaults to unset.
     */

    settings.add(
        setting(
            "otel.experimental.config.file",
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
    // https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/#db-statement-sanitization

    /*
    System property: otel.instrumentation.common.db-statement-sanitizer.enabled
    Environment variable: OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED
    Default: true
    Description: Enables the DB statement sanitization.
     */

    settings.add(
        setting(
            "otel.instrumentation.common.db-statement-sanitizer.enabled",
            "Enables the DB statement sanitization.",
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
    settings.add(
        setting(
            "otel.instrumentation.http.client.experimental.redact-query-parameters",
            "Redact sensitive parameter values from URL query string, see https://opentelemetry.io/docs/specs/semconv/http/http-spans.",
            "true",
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

    // https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md

    // Splunk configuration
    // https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#splunk-configuration

    /*
    | `splunk.access.token`                   | `SPLUNK_ACCESS_TOKEN`                   | unset                   | Stable       | (Optional) Auth token allowing exporters to communicate directly with the Splunk cloud, passed as `X-SF-TOKEN` header. Currently, the [SignalFx metrics exporter](metrics.md) supports this property.                |
    | `splunk.realm`                          | `SPLUNK_REALM`                          | `none`                  | Stable       | The Splunk Observability Cloud realm where the telemetry should be sent to. For example, `us0` or `us1`. Defaults to `none`, which means that data goes to a Splunk OpenTelemetry Collector deployed on `localhost`. |
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
            "The Splunk Observability Cloud realm where the telemetry should be sent to. For example, us0 or us1. Defaults to none, which means that data goes to a Splunk OpenTelemetry Collector deployed on localhost.",
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

    // instrumentation specific configuration

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/aws-sdk/README.md
    /*
    | `otel.instrumentation.aws-sdk.experimental-span-attributes`              | Boolean | `false` | Enable the capture of experimental span attributes.                                         |
    | `otel.instrumentation.aws-sdk.experimental-use-propagator-for-messaging` | Boolean | `false` | v2 only, inject into SNS/SQS attributes with configured propagator: See [v2 README](aws-sdk-2.2/library/README.md#trace-propagation). |
    | `otel.instrumentation.aws-sdk.experimental-record-individual-http-error` | Boolean | `false` | v2 only, record errors returned by each individual HTTP request as events for the SDK span. |
    | `otel.instrumentation.genai.capture-message-content`                     | Boolean | `false` | v2 only, record content of user and LLM messages when using Bedrock.                                                                  |
     */
    settings.add(
        setting(
            "otel.instrumentation.aws-sdk.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.aws-sdk.experimental-use-propagator-for-messaging",
            "v2 only, inject into SNS/SQS attributes with configured propagator.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.aws-sdk.experimental-use-propagator-for-messaging",
            "v2 only, record errors returned by each individual HTTP request as events for the SDK span.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.genai.capture-message-content",
            "v2 only, record content of user and LLM messages when using Bedrock.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/camel-2.20/README.md
    /*
    | `otel.instrumentation.camel.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.camel.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/apache-shenyu-2.4/README.md
    /*
    | `otel.instrumentation.apache-shenyu.experimental-span-attributes`   | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.apache-shenyu.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/couchbase/README.md
    /*
    | `otel.instrumentation.couchbase.experimental-span-attributes` | Boolean | `false` | Enables the capture of experimental span attributes (for version 2.6 and higher of this instrumentation). |
     */
    settings.add(
        setting(
            "otel.instrumentation.couchbase.experimental-span-attributes",
            "Enables the capture of experimental span attributes (for version 2.6 and higher of this instrumentation).",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/elasticsearch/README.md
    /*
    | `otel.instrumentation.elasticsearch.capture-search-query` | Boolean | `false` | Enable the capture of search query bodies. Attention: Elasticsearch queries may contain personal or sensitive information. |
    | `otel.instrumentation.elasticsearch.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.elasticsearch.capture-search-query",
            "Enable the capture of search query bodies. Attention: Elasticsearch queries may contain personal or sensitive information.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.elasticsearch.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/graphql-java/README.md
    /*
    | `otel.instrumentation.graphql.query-sanitizer.enabled` | Boolean | `true`  | Whether to remove sensitive information from query source that is added as span attribute. |
    | `otel.instrumentation.graphql.add-operation-name-to-span-name.enabled` | Boolean | `false` | Whether GraphQL operation name is added to the span name. <p>**WARNING**: GraphQL operation name is provided by the client and can have high cardinality. Use only when the server is not exposed to malicious clients. |
     */
    // graphql 20
    /*
    | `otel.instrumentation.graphql.data-fetcher.enabled`         | Boolean | `false` | Whether to create spans for data fetchers.                                                                                        |
    | `otel.instrumentation.graphql.trivial-data-fetcher.enabled` | Boolean | `false` | Whether to create spans for trivial data fetchers. A trivial data fetcher is one that simply maps data from an object to a field. |
     */
    settings.add(
        setting(
            "otel.instrumentation.graphql.query-sanitizer.enabled",
            "Whether to remove sensitive information from query source that is added as span attribute.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.graphql.add-operation-name-to-span-name.enabled",
            "Whether GraphQL operation name is added to the span name. <p>**WARNING**: GraphQL operation name is provided by the client and can have high cardinality. Use only when the server is not exposed to malicious clients.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.graphql.data-fetcher.enabled",
            "Whether to create spans for data fetchers (GraphQL 20 and later).",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.graphql.trivial-data-fetcher.enabled",
            "Whether to create spans for trivial data fetchers. A trivial data fetcher is one that simply maps data from an object to a field (GraphQL 20 and later).",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/grpc-1.6/README.md
    /*
    | `otel.instrumentation.grpc.emit-message-events`             | Boolean | `true`  | Determines whether to emit span event for each individual message received and sent.
    | `otel.instrumentation.grpc.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
    | `otel.instrumentation.grpc.capture-metadata.client.request` | String  |         | A comma-separated list of request metadata keys. gRPC client instrumentation will capture metadata values corresponding to configured keys as span attributes. |
    | `otel.instrumentation.grpc.capture-metadata.server.request` | String  |         | A comma-separated list of request metadata keys. gRPC server instrumentation will capture metadata values corresponding to configured keys as span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.grpc.emit-message-events",
            "Determines whether to emit span event for each individual message received and sent.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.grpc.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.grpc.capture-metadata.client.request",
            "A comma-separated list of request metadata keys. gRPC client instrumentation will capture metadata values corresponding to configured keys as span attributes.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.grpc.capture-metadata.server.request",
            "A comma-separated list of request metadata keys. gRPC server instrumentation will capture metadata values corresponding to configured keys as span attributes.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/guava-10.0/README.md
    /*
    | `otel.instrumentation.guava.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.guava.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/hibernate/README.md
    /*
    | `otel.instrumentation.hibernate.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.hibernate.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/hystrix-1.4/javaagent/README.md
    /*
    | `otel.instrumentation.hystrix.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.hystrix.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/java-util-logging/javaagent/README.md
    /*
    | `otel.instrumentation.java-util-logging.experimental-log-attributes` | Boolean | `false` | Enable the capture of experimental log attributes `thread.name` and `thread.id`. |
     */
    settings.add(
        setting(
            "otel.instrumentation.java-util-logging.experimental-log-attributes",
            "Enable the capture of experimental log attributes `thread.name` and `thread.id`.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/jaxrs/README.md
    /*
    | `otel.instrumentation.jaxrs.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.jaxrs.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/jboss-logmanager/README.md
    /*
    | `otel.instrumentation.jboss-logmanager.experimental-log-attributes`         | Boolean | `false` | Enable the capture of experimental log attributes.                                                           |
    | `otel.instrumentation.jboss-logmanager.experimental.capture-mdc-attributes` | String  |         | Comma separated list of MDC attributes to capture. Use the wildcard character `*` to capture all attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.jboss-logmanager.experimental-log-attributes",
            "Enable the capture of experimental log attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.jboss-logmanager.experimental.capture-mdc-attributes",
            "Comma separated list of MDC attributes to capture. Use the wildcard character `*` to capture all attributes.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/jdbc/README.md
    /*
    | `otel.instrumentation.jdbc.statement-sanitizer.enabled` | Boolean | `true`  | Enables the DB statement sanitization. |
    | `otel.instrumentation.jdbc.experimental.capture-query-parameters` | Boolean | `false` | Enable the capture of query parameters as span attributes. Enabling this option disables the statement sanitization. <p>WARNING: captured query parameters may contain sensitive information such as passwords, personally identifiable information or protected health info. |
    | `otel.instrumentation.jdbc.experimental.transaction.enabled` | Boolean | `false` | Enables experimental instrumentation to create spans for COMMIT and ROLLBACK operations. |
     */
    settings.add(
        setting(
            "otel.instrumentation.jdbc.statement-sanitizer.enabled",
            "Enables the DB statement sanitization.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.jdbc.experimental.capture-query-parameters",
            "Enable the capture of query parameters as span attributes. Enabling this option disables the statement sanitization. <p>WARNING: captured query parameters may contain sensitive information such as passwords, personally identifiable information or protected health info.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.jdbc.experimental.transaction.enabled",
            "Enables experimental instrumentation to create spans for COMMIT and ROLLBACK operations.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/jsp-2.3/README.md
    /*
    | `otel.instrumentation.jsp.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.jsp.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/kafka/README.md
    /*
    | `otel.instrumentation.kafka.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes.                                                                        |
    | `otel.instrumentation.kafka.producer-propagation.enabled` | Boolean | `true`  | Enable context propagation for kafka message producer.                                                                     |
     */
    settings.add(
        setting(
            "otel.instrumentation.kafka.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.kafka.producer-propagation.enabled",
            "Enable context propagation for kafka message producer.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/kubernetes-client-7.0/README.md
    /*
    | `otel.instrumentation.kubernetes-client.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.kubernetes-client.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/lettuce/README.md
    /*
    | `otel.instrumentation.lettuce.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.lettuce.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/log4j/log4j-appender-2.17/javaagent/README.md
    /*
     | `otel.instrumentation.log4j-appender.experimental-log-attributes`                 | Boolean | `false` | Enable the capture of experimental log attributes `thread.name` and `thread.id`.                                                              |
     | `otel.instrumentation.log4j-appender.experimental.capture-code-attributes`        | Boolean | `false` | Enable the capture of [source code attributes]. Note that capturing source code attributes at logging sites might add a performance overhead. |
     | `otel.instrumentation.log4j-appender.experimental.capture-map-message-attributes` | Boolean | `false` | Enable the capture of `MapMessage` attributes.                                                                                                |
     | `otel.instrumentation.log4j-appender.experimental.capture-marker-attribute`       | Boolean | `false` | Enable the capture of Log4j markers as attributes.                                                                                            |
     | `otel.instrumentation.log4j-appender.experimental.capture-mdc-attributes`         | String  |         | Comma separated list of context data attributes to capture. Use the wildcard character `*` to capture all attributes.                         |
    */
    settings.add(
        setting(
            "otel.instrumentation.log4j-appender.experimental-log-attributes",
            "Enable the capture of experimental log attributes `thread.name` and `thread.id`.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.log4j-appender.experimental.capture-code-attributes",
            "Enable the capture of [source code attributes](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/attributes.md#source-code-attributes). Note that capturing source code attributes at logging sites might add a performance overhead.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.log4j-appender.experimental.capture-map-message-attributes",
            "Enable the capture of `MapMessage` attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.log4j-appender.experimental.capture-marker-attribute",
            "Enable the capture of Log4j markers as attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.log4j-appender.experimental.capture-mdc-attributes",
            "Comma separated list of context data attributes to capture. Use the wildcard character `*` to capture all attributes.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/log4j/log4j-context-data/log4j-context-data-2.17/javaagent/README.md
    /*
    | `otel.instrumentation.log4j-context-data.add-baggage` | Boolean | `false`       | Enable exposing baggage attributes through MDC. |
     */
    settings.add(
        setting(
            "otel.instrumentation.log4j-context-data.add-baggage",
            "Enable exposing baggage attributes through MDC.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/log4j/log4j-context-data/log4j-context-data-2.17/javaagent/README.md
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/log4j/log4j-mdc-1.2/javaagent/README.md
    /*
    | `otel.instrumentation.common.mdc.resource-attributes` | String  |               | Comma separated list of resource attributes to expose through MDC. |
    | `otel.instrumentation.common.logging.trace-id`        | String  | `trace_id`    | Customize MDC key name for the trace id.                           |
    | `otel.instrumentation.common.logging.span-id`         | String  | `span_id`     | Customize MDC key name for the span id.                            |
    | `otel.instrumentation.common.logging.trace-flags`     | String  | `trace_flags` | Customize MDC key name for the trace flags.                        |
     */
    settings.add(
        setting(
            "otel.instrumentation.common.mdc.resource-attributes",
            "Comma separated list of resource attributes to expose through MDC.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.logging.trace-id",
            "Customize MDC key name for the trace id.",
            "trace_id",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.logging.span-id",
            "Customize MDC key name for the span id.",
            "span_id",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.logging.trace-flags",
            "Customize MDC key name for the trace flags.",
            "trace_flags",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/logback/logback-appender-1.0/javaagent/README.md
    /*
    | `otel.instrumentation.logback-appender.experimental-log-attributes`                    | Boolean | `false` | Enable the capture of experimental log attributes `thread.name` and `thread.id`.                                                              |
    | `otel.instrumentation.logback-appender.experimental.capture-code-attributes`           | Boolean | `false` | Enable the capture of [source code attributes]. Note that capturing source code attributes at logging sites might add a performance overhead. |
    | `otel.instrumentation.logback-appender.experimental.capture-marker-attribute`          | Boolean | `false` | Enable the capture of Logback markers as attributes.                                                                                          |
    | `otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes` | Boolean | `false` | Enable the capture of Logback key value pairs as attributes.                                                                                  |
    | `otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes` | Boolean | `false` | Enable the capture of Logback logger context properties as attributes.                                                                        |
    | `otel.instrumentation.logback-appender.experimental.capture-arguments`                 | Boolean | `false` | Enable the capture of Logback logger arguments.                                                                                               |
    | `otel.instrumentation.logback-appender.experimental.capture-mdc-attributes`            | String  |         | Comma separated list of MDC attributes to capture. Use the wildcard character `*` to capture all attributes.                                  |
     */
    settings.add(
        setting(
            "otel.instrumentation.logback-appender.experimental-log-attributes",
            "Enable the capture of experimental log attributes `thread.name` and `thread.id`.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.logback-appender.experimental.capture-code-attributes",
            "Enable the capture of [source code attributes](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/general/attributes.md#source-code-attributes). Note that capturing source code attributes at logging sites might add a performance overhead.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.logback-appender.experimental.capture-marker-attribute",
            "Enable the capture of Logback markers as attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes",
            "Enable the capture of Logback key value pairs as attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes",
            "Enable the capture of Logback logger context properties as attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.logback-appender.experimental.capture-arguments",
            "Enable the capture of Logback logger arguments.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.logback-appender.experimental.capture-mdc-attributes",
            "Comma separated list of MDC attributes to capture. Use the wildcard character `*` to capture all attributes.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/logback/logback-mdc-1.0/javaagent/README.md
    /*
    | `otel.instrumentation.logback-mdc.add-baggage`        | Boolean | `false` | Enable exposing baggage attributes through MDC.                    |
    | `otel.instrumentation.common.mdc.resource-attributes` | String  |         | Comma separated list of resource attributes to expose through MDC. |
     */
    settings.add(
        setting(
            "otel.instrumentation.logback-mdc.add-baggage",
            "Enable exposing baggage attributes through MDC.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.common.mdc.resource-attributes",
            "Comma separated list of resource attributes to expose through MDC.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/methods/README.md
    /*
    | `otel.instrumentation.methods.include` | String | None    | List of methods to include for tracing. For more information, see [Creating spans around methods with `otel.instrumentation.methods.include`][cs]. |

    [cs]: https://opentelemetry.io/docs/instrumentation/java/annotations/#creating-spans-around-methods-with-otelinstrumentationmethodsinclude
     */
    // already added
    /*
    settings.add(setting(toEnvVar("otel.instrumentation.methods.include"),
            "List of methods to include for tracing. For more information, see [Creating spans around methods with `otel.instrumentation.methods.include`][https://opentelemetry.io/docs/instrumentation/java/annotations/#creating-spans-around-methods-with-otelinstrumentationmethodsinclude].",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
     */

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/micrometer/micrometer-1.5/javaagent/README.md
    /*
    | `otel.instrumentation.micrometer.base-time-unit`           | String  | `s`     | Set the base time unit for the OpenTelemetry `MeterRegistry` implementation. <details><summary>Valid values</summary>`ns`, `nanoseconds`, `us`, `microseconds`, `ms`, `milliseconds`, `s`, `seconds`, `min`, `minutes`, `h`, `hours`, `d`, `days`</details> |
    | `otel.instrumentation.micrometer.prometheus-mode.enabled`  | boolean | false   | Enable the "Prometheus mode" this will simulate the behavior of Micrometer's PrometheusMeterRegistry. The instruments will be renamed to match Micrometer instrument naming, and the base time unit will be set to seconds.                                 |
    | `otel.instrumentation.micrometer.histogram-gauges.enabled` | boolean | false   | Enables the generation of gauge-based Micrometer histograms for `DistributionSummary` and `Timer` instruments.                                                                                                                                              |
     */
    settings.add(
        setting(
            "otel.instrumentation.micrometer.base-time-unit",
            "Set the base time unit for the OpenTelemetry `MeterRegistry` implementation. <details><summary>Valid values</summary>`ns`, `nanoseconds`, `us`, `microseconds`, `ms`, `milliseconds`, `s`, `seconds`, `min`, `minutes`, `h`, `hours`, `d`, `days`</details>",
            "s",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.micrometer.prometheus-mode.enabled",
            "Enable the \"Prometheus mode\" this will simulate the behavior of Micrometer's PrometheusMeterRegistry. The instruments will be renamed to match Micrometer instrument naming, and the base time unit will be set to seconds.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.micrometer.histogram-gauges.enabled",
            "Enables the generation of gauge-based Micrometer histograms for `DistributionSummary` and `Timer` instruments.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/mongo/README.md
    /*
    | `otel.instrumentation.mongo.statement-sanitizer.enabled` | Boolean | `true`  | Enables the DB statement sanitization. |
     */
    settings.add(
        setting(
            "otel.instrumentation.mongo.statement-sanitizer.enabled",
            "Enables the DB statement sanitization.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/netty/README.md
    /*
    | `otel.instrumentation.netty.connection-telemetry.enabled` | Boolean | `false` | Enable the creation of Connect and DNS spans by default for Netty 4.0 and higher instrumentation. |
    | `otel.instrumentation.netty.ssl-telemetry.enabled`        | Boolean | `false` | Enable SSL telemetry for Netty 4.0 and higher instrumentation.                                    |
     */
    settings.add(
        setting(
            "otel.instrumentation.netty.connection-telemetry.enabled",
            "Enable the creation of Connect and DNS spans by default for Netty 4.0 and higher instrumentation.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.netty.ssl-telemetry.enabled",
            "Enable SSL telemetry for Netty 4.0 and higher instrumentation.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/openai/openai-java-1.1/javaagent/README.md
    /*
    | `otel.instrumentation.genai.capture-message-content` | Boolean | `false` | Record content of user and LLM messages. |
     */
    settings.add(
        setting(
            "otel.instrumentation.genai.capture-message-content",
            "Record content of user and LLM messages.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/opentelemetry-extension-annotations-1.0/README.md
    /*
    | `otel.instrumentation.opentelemetry-annotations.exclude-methods` | String |         | All methods to be excluded from auto-instrumentation by annotation-based advices. |
     */
    settings.add(
        setting(
            "otel.instrumentation.opentelemetry-annotations.exclude-methods",
            "All methods to be excluded from auto-instrumentation by annotation-based advices.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/opentelemetry-instrumentation-annotations-1.16/README.md
    /*
    | `otel.instrumentation.opentelemetry-instrumentation-annotations.exclude-methods` | String |         | All methods to be excluded from auto-instrumentation by annotation-based advices. |
     */
    settings.add(
        setting(
            "otel.instrumentation.opentelemetry-instrumentation-annotations.exclude-methods",
            "All methods to be excluded from auto-instrumentation by annotation-based advices.",
            "",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/oshi/README.md
    /*
    | `otel.instrumentation.oshi.experimental-metrics.enabled`  | Boolean | `false` | Enable the OSHI metrics. |
     */
    settings.add(
        setting(
            "otel.instrumentation.oshi.experimental-metrics.enabled",
            "Enable the OSHI metrics.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/powerjob-4.0/README.md
    /*
    | `otel.instrumentation.powerjob.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.powerjob.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/pulsar/pulsar-2.8/README.md
    /*
    | `otel.instrumentation.pulsar.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.pulsar.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/quartz-2.0/README.md
    /*
    | `otel.instrumentation.quartz.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.quartz.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/r2dbc-1.0/README.md
    /*
    | `otel.instrumentation.r2dbc.statement-sanitizer.enabled` | Boolean | `true`  | Enables the DB statement sanitization. |
     */
    settings.add(
        setting(
            "otel.instrumentation.r2dbc.statement-sanitizer.enabled",
            "Enables the DB statement sanitization.",
            "true",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/rabbitmq-2.7/README.md
    /*
    | `otel.instrumentation.rabbitmq.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.rabbitmq.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/reactor/reactor-3.1/README.md
    /*
    | `otel.instrumentation.reactor.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.reactor.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/reactor/reactor-netty/README.md
    /*
    | `otel.instrumentation.reactor-netty.connection-telemetry.enabled` | Boolean | `false` | Enable the creation of Connect and DNS spans by default. |
     */
    settings.add(
        setting(
            "otel.instrumentation.reactor-netty.connection-telemetry.enabled",
            "Enable the creation of Connect and DNS spans by default.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/rocketmq/rocketmq-client/rocketmq-client-4.8/README.md
    /*
    | `otel.instrumentation.rocketmq-client.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.rocketmq-client.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/runtime-telemetry/README.md
    /*
    | `otel.instrumentation.runtime-telemetry.emit-experimental-telemetry`     | Boolean | `false` | Enable the capture of experimental metrics.                       |
    | `otel.instrumentation.runtime-telemetry-java17.enable-all`               | Boolean | `false` | Enable the capture of all JFR based metrics.                      |
    | `otel.instrumentation.runtime-telemetry-java17.enabled`                  | Boolean | `false` | Enable the capture of JFR based metrics.                          |
    | `otel.instrumentation.runtime-telemetry.package-emitter.enabled`         | Boolean | `false` | Enable creating events for JAR libraries used by the application. |
    | `otel.instrumentation.runtime-telemetry.package-emitter.jars-per-second` | Integer | 10      | The number of JAR files processed per second.                     |
     */
    settings.add(
        setting(
            "otel.instrumentation.runtime-telemetry.emit-experimental-telemetry",
            "Enable the capture of experimental metrics.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.runtime-telemetry-java17.enable-all",
            "Enable the capture of all JFR based metrics.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.runtime-telemetry-java17.enabled",
            "Enable the capture of JFR based metrics.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.runtime-telemetry.package-emitter.enabled",
            "Enable creating events for JAR libraries used by the application.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.runtime-telemetry.package-emitter.jars-per-second",
            "The number of JAR files processed per second.",
            "10",
            SettingType.INT,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/rxjava/README.md
    /*
    | `otel.instrumentation.rxjava.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes for RxJava 2 and 3 instrumentation. |
     */
    settings.add(
        setting(
            "otel.instrumentation.rxjava.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/servlet/README.md
    /*
    | `otel.instrumentation.servlet.experimental-span-attributes`            | Boolean | `false` | Enable the capture of experimental span attributes. |
    | `otel.instrumentation.servlet.experimental.capture-request-parameters` | List    | Empty   | Request parameters to be captured (experimental).   |
     */
    settings.add(
        setting(
            "otel.instrumentation.servlet.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    // otel.instrumentation.servlet.experimental.capture-request-parameters is already added
    // elsewhere

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/spring/README.md
    /*
    | `otel.instrumentation.spring-batch.item.enabled`                              | Boolean | `false` | Enable creating span for each item.                                                                                                                                                                                                                                                                                                                     |
    | `otel.instrumentation.spring-batch.experimental.chunk.new-trace`              | Boolean | `false` | Enable staring a new trace for each chunk.                                                                                                                                                                                                                                                                                                              |
    | `otel.instrumentation.spring-batch.experimental-span-attributes`              | Boolean | `false` | Enable the capture of experimental span attributes for Spring Batch version 3.0.                                                                                                                                                                                                                                                                        |
    | `otel.instrumentation.spring-integration.global-channel-interceptor-patterns` | List    | `*`     | An array of Spring channel name patterns that will be intercepted. See [Spring Integration docs](https://docs.spring.io/spring-integration/reference/channel/configuration.html#global-channel-configuration-interceptors) for more details.                                                                                                            |
    | `otel.instrumentation.spring-integration.producer.enabled`                    | Boolean | `false` | Create producer spans when messages are sent to an output channel. Enable when you're using a messaging library that doesn't have its own instrumentation for generating producer spans. Note that the detection of output channels only works for [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) `DirectWithAttributesChannel`. |
    | `otel.instrumentation.spring-scheduling.experimental-span-attributes`         | Boolean | `false` | Enable the capture of experimental span attributes for Spring Scheduling version 3.1.                                                                                                                                                                                                                                                                   |
    | `otel.instrumentation.spring-webflux.experimental-span-attributes`            | Boolean | `false` | Enable the capture of experimental span attributes for Spring WebFlux version 5.0.                                                                                                                                                                                                                                                                      |
    | `otel.instrumentation.spring-webmvc.experimental-span-attributes`             | Boolean | `false` | Enable the capture of experimental span attributes for Spring Web MVC version 3.1.                                                                                                                                                                                                                                                                      |
     */
    settings.add(
        setting(
            "otel.instrumentation.spring-batch.item.enabled",
            "Enable creating span for each item.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-batch.experimental.chunk.new-trace",
            "Enable staring a new trace for each chunk.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-scheduling.experimental-span-attributes",
            "Enable the capture of experimental span attributes for Spring Batch version 3.0.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-integration.global-channel-interceptor-patterns",
            "An array of Spring channel name patterns that will be intercepted. See [Spring Integration docs](https://docs.spring.io/spring-integration/reference/channel/configuration.html#global-channel-configuration-interceptors) for more details.",
            "*",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-integration.producer.enabled",
            "Create producer spans when messages are sent to an output channel. Enable when you're using a messaging library that doesn't have its own instrumentation for generating producer spans. Note that the detection of output channels only works for [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) `DirectWithAttributesChannel`.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-scheduling.experimental-span-attributes",
            "Enable the capture of experimental span attributes for Spring Scheduling version 3.1.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-webflux.experimental-span-attributes",
            "Enable the capture of experimental span attributes for Spring WebFlux version 5.0.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-webmvc.experimental-span-attributes",
            "Enable the capture of experimental span attributes for Spring Web MVC version 3.1.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/spring/spring-cloud-gateway/README.md
    /*
    | `otel.instrumentation.spring-cloud-gateway.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes.                                         |
     */
    settings.add(
        setting(
            "otel.instrumentation.spring-cloud-gateway.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/spring/spring-security-config-6.0/javaagent/README.md
    /*
    | `otel.instrumentation.spring-security.enduser.role.granted-authority-prefix`  | String  | `ROLE_`  | Prefix of granted authorities identifying roles to capture in the `enduser.role` semantic attribute.    |
    | `otel.instrumentation.spring-security.enduser.scope.granted-authority-prefix` | String  | `SCOPE_` | Prefix of granted authorities identifying scopes to capture in the `enduser.scopes` semantic attribute. |
     */
    settings.add(
        setting(
            "otel.instrumentation.spring-security.enduser.role.granted-authority-prefix",
            "Prefix of granted authorities identifying roles to capture in the `enduser.role` semantic attribute.",
            "ROLE_",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));
    settings.add(
        setting(
            "otel.instrumentation.spring-security.enduser.scope.granted-authority-prefix",
            "Prefix of granted authorities identifying scopes to capture in the `enduser.scopes` semantic attribute.",
            "SCOPE_",
            SettingType.STRING,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/spymemcached-2.12/README.md
    /*
    | `otel.instrumentation.spymemcached.experimental-span-attributes` | Boolean | `false` | Enables the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.spymemcached.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/twilio-6.6/README.md
    /*
    | `otel.instrumentation.twilio.experimental-span-attributes` | Boolean | `false` | Enables the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.twilio.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/xxl-job/README.md
    /*
    | `otel.instrumentation.xxl-job.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes. |
     */
    settings.add(
        setting(
            "otel.instrumentation.xxl-job.experimental-span-attributes",
            "Enable the capture of experimental span attributes.",
            "false",
            SettingType.BOOLEAN,
            SettingCategory.INSTRUMENTATION));

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

    instrumentations.add(
        instrumentation("activej-http")
            .component("ActiveJ HTTP Server", "6.0 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("methods").component("Additional methods tracing", null).build());
    instrumentations.add(
        instrumentation("external-annotations")
            .component("Additional tracing annotations", null)
            .build());
    instrumentations.add(
        instrumentation("akka-actor").component("Akka Actors", "2.3 and higher").build());
    instrumentations.add(
        instrumentation("akka-http")
            .component("Akka HTTP", "10.0 and higher")
            .httpClientMetrics()
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("axis2").component("Apache Axis2", "1.6 and higher").build());
    instrumentations.add(instrumentation("camel").component("Apache Camel", "2.20 to 3.0").build());
    instrumentations.add(
        instrumentation("cxf")
            .component("Apache CXF JAX-RS", "3.2 and higher")
            .component("Apache CXF JAX-WS", "3.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("apache-dbcp")
            .component("Apache DBCP", "2.0 and higher")
            .dbPoolMetrics(
                DbPoolMetrics.USAGE,
                DbPoolMetrics.IDLE_MAX,
                DbPoolMetrics.IDLE_MIN,
                DbPoolMetrics.MAX)
            .build());
    instrumentations.add(
        instrumentation("apache-dubbo").component("Apache DBCP", "2.7 and higher").build());
    instrumentations.add(
        instrumentation("apache-httpasyncclient")
            .component("Apache HttpAsyncClient", "4.1 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("apache-httpclient")
            .component("Apache HttpClient", "2.0 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation(
                List.of("kafka", "kafka-clients-metrics"),
                "Consult documentation of the used kafka version for the exact metrics it generates.")
            .component("Apache Kafka Producer/Consumer API", "0.11 and higher")
            .component("Apache Kafka Streams API", "0.11 and higher")
            // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/kafka/kafka-clients/kafka-clients-2.6/library/README.md
            // these metrics are created by kafka, we only collect these as otel metrics
            .customMetric(
                "kafka.consumer.assigned_partitions",
                MetricInstrument.GAUGE,
                "The number of partitions currently assigned to this consumer.")
            .customMetric(
                "kafka.consumer.commit_latency_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a commit request.")
            .customMetric(
                "kafka.consumer.commit_latency_max",
                MetricInstrument.GAUGE,
                "The max time taken for a commit request.")
            .customMetric(
                "kafka.consumer.commit_rate",
                MetricInstrument.GAUGE,
                "The number of commit calls per second.")
            .customMetric(
                "kafka.consumer.commit_total",
                MetricInstrument.COUNTER,
                "The total number of commit calls.")
            .customMetric(
                "kafka.consumer.failed_rebalance_rate_per_hour",
                MetricInstrument.GAUGE,
                "The number of failed rebalance events per hour.")
            .customMetric(
                "kafka.consumer.failed_rebalance_total",
                MetricInstrument.COUNTER,
                "The total number of failed rebalance events.")
            .customMetric(
                "kafka.consumer.heartbeat_rate",
                MetricInstrument.GAUGE,
                "The number of heartbeats per second.")
            .customMetric(
                "kafka.consumer.heartbeat_response_time_max",
                MetricInstrument.GAUGE,
                "The max time taken to receive a response to a heartbeat request.")
            .customMetric(
                "kafka.consumer.heartbeat_total",
                MetricInstrument.COUNTER,
                "The total number of heartbeats.")
            .customMetric(
                "kafka.consumer.join_rate",
                MetricInstrument.GAUGE,
                "The number of group joins per second.")
            .customMetric(
                "kafka.consumer.join_time_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a group rejoin.")
            .customMetric(
                "kafka.consumer.join_time_max",
                MetricInstrument.GAUGE,
                "The max time taken for a group rejoin.")
            .customMetric(
                "kafka.consumer.join_total",
                MetricInstrument.COUNTER,
                "The total number of group joins.")
            .customMetric(
                "kafka.consumer.last_heartbeat_seconds_ago",
                MetricInstrument.GAUGE,
                "The number of seconds since the last coordinator heartbeat was sent.")
            .customMetric(
                "kafka.consumer.last_rebalance_seconds_ago",
                MetricInstrument.GAUGE,
                "The number of seconds since the last successful rebalance event.")
            .customMetric(
                "kafka.consumer.partition_assigned_latency_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a partition-assigned rebalance listener callback.")
            .customMetric(
                "kafka.consumer.partition_assigned_latency_max",
                MetricInstrument.GAUGE,
                "The max time taken for a partition-assigned rebalance listener callback.")
            .customMetric(
                "kafka.consumer.partition_lost_latency_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a partition-lost rebalance listener callback.")
            .customMetric(
                "kafka.consumer.partition_lost_latency_max",
                MetricInstrument.GAUGE,
                "The max time taken for a partition-lost rebalance listener callback.")
            .customMetric(
                "kafka.consumer.partition_revoked_latency_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a partition-revoked rebalance listener callback.")
            .customMetric(
                "kafka.consumer.partition_revoked_latency_max",
                MetricInstrument.GAUGE,
                "The max time taken for a partition-revoked rebalance listener callback.")
            .customMetric(
                "kafka.consumer.rebalance_latency_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a group to complete a successful rebalance, which may be composed of several failed re-trials until it succeeded.")
            .customMetric(
                "kafka.consumer.rebalance_latency_max",
                MetricInstrument.GAUGE,
                "The max time taken for a group to complete a successful rebalance, which may be composed of several failed re-trials until it succeeded.")
            .customMetric(
                "kafka.consumer.rebalance_latency_total",
                MetricInstrument.COUNTER,
                "The total number of milliseconds this consumer has spent in successful rebalances since creation.")
            .customMetric(
                "kafka.consumer.rebalance_rate_per_hour",
                MetricInstrument.GAUGE,
                "The number of successful rebalance events per hour, each event is composed of several failed re-trials until it succeeded.")
            .customMetric(
                "kafka.consumer.rebalance_total",
                MetricInstrument.COUNTER,
                "The total number of successful rebalance events, each event is composed of several failed re-trials until it succeeded.")
            .customMetric(
                "kafka.consumer.sync_rate",
                MetricInstrument.GAUGE,
                "The number of group syncs per second.")
            .customMetric(
                "kafka.consumer.sync_time_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a group sync.")
            .customMetric(
                "kafka.consumer.sync_time_max",
                MetricInstrument.GAUGE,
                "The max time taken for a group sync.")
            .customMetric(
                "kafka.consumer.sync_total",
                MetricInstrument.COUNTER,
                "The total number of group syncs.")
            .customMetric(
                "kafka.consumer.bytes_consumed_rate",
                MetricInstrument.GAUGE,
                "The average number of bytes consumed per second.")
            .customMetric(
                "kafka.consumer.bytes_consumed_total",
                MetricInstrument.COUNTER,
                "The total number of bytes consumed.")
            .customMetric(
                "kafka.consumer.fetch_latency_avg",
                MetricInstrument.GAUGE,
                "The average time taken for a fetch request.")
            .customMetric(
                "kafka.consumer.fetch_latency_max",
                MetricInstrument.GAUGE,
                "The max time taken for any fetch request.")
            .customMetric(
                "kafka.consumer.fetch_rate",
                MetricInstrument.GAUGE,
                "The number of fetch requests per second.")
            .customMetric(
                "kafka.consumer.fetch_size_avg",
                MetricInstrument.GAUGE,
                "The average number of bytes fetched per request.")
            .customMetric(
                "kafka.consumer.fetch_size_max",
                MetricInstrument.GAUGE,
                "The maximum number of bytes fetched per request.")
            .customMetric(
                "kafka.consumer.fetch_throttle_time_avg",
                MetricInstrument.GAUGE,
                "The average throttle time in ms.")
            .customMetric(
                "kafka.consumer.fetch_throttle_time_max",
                MetricInstrument.GAUGE,
                "The maximum throttle time in ms.")
            .customMetric(
                "kafka.consumer.fetch_total",
                MetricInstrument.COUNTER,
                "The total number of fetch requests.")
            .customMetric(
                "kafka.consumer.records_consumed_rate",
                MetricInstrument.GAUGE,
                "The average number of records consumed per second.")
            .customMetric(
                "kafka.consumer.records_consumed_total",
                MetricInstrument.COUNTER,
                "The total number of records consumed.")
            .customMetric(
                "kafka.consumer.records_lag",
                MetricInstrument.GAUGE,
                "The latest lag of the partition.")
            .customMetric(
                "kafka.consumer.records_lag_avg",
                MetricInstrument.GAUGE,
                "The average lag of the partition.")
            .customMetric(
                "kafka.consumer.records_lag_max",
                MetricInstrument.GAUGE,
                "The maximum lag in terms of number of records for any partition in this window. NOTE: This is based on current offset and not committed offset.")
            .customMetric(
                "kafka.consumer.records_lead",
                MetricInstrument.GAUGE,
                "The latest lead of the partition.")
            .customMetric(
                "kafka.consumer.records_lead_avg",
                MetricInstrument.GAUGE,
                "The average lead of the partition.")
            .customMetric(
                "kafka.consumer.records_lead_min",
                MetricInstrument.GAUGE,
                "The minimum lead in terms of number of records for any partition in this window.")
            .customMetric(
                "kafka.consumer.records_per_request_avg",
                MetricInstrument.GAUGE,
                "The average number of records in each request.")
            .customMetric(
                "kafka.consumer.commit_sync_time_ns_total",
                MetricInstrument.COUNTER,
                "The total time the consumer has spent in commitSync in nanoseconds.")
            .customMetric(
                "kafka.consumer.committed_time_ns_total",
                MetricInstrument.COUNTER,
                "The total time the consumer has spent in committed in nanoseconds.")
            .customMetric(
                "kafka.consumer.connection_close_rate",
                MetricInstrument.GAUGE,
                "The number of connections closed per second.")
            .customMetric(
                "kafka.consumer.connection_close_total",
                MetricInstrument.COUNTER,
                "The total number of connections closed.")
            .customMetric(
                "kafka.consumer.connection_count",
                MetricInstrument.GAUGE,
                "The current number of active connections.")
            .customMetric(
                "kafka.consumer.connection_creation_rate",
                MetricInstrument.GAUGE,
                "The number of new connections established per second.")
            .customMetric(
                "kafka.consumer.connection_creation_total",
                MetricInstrument.COUNTER,
                "The total number of new connections established.")
            .customMetric(
                "kafka.consumer.failed_authentication_rate",
                MetricInstrument.GAUGE,
                "The number of connections with failed authentication per second.")
            .customMetric(
                "kafka.consumer.failed_authentication_total",
                MetricInstrument.COUNTER,
                "The total number of connections with failed authentication.")
            .customMetric(
                "kafka.consumer.failed_reauthentication_rate",
                MetricInstrument.GAUGE,
                "The number of failed re-authentication of connections per second.")
            .customMetric(
                "kafka.consumer.failed_reauthentication_total",
                MetricInstrument.COUNTER,
                "The total number of failed re-authentication of connections.")
            .customMetric(
                "kafka.consumer.io_ratio",
                MetricInstrument.GAUGE,
                " *Deprecated* The fraction of time the I/O thread spent doing I/O.")
            .customMetric(
                "kafka.consumer.io_time_ns_avg",
                MetricInstrument.GAUGE,
                "The average length of time for I/O per select call in nanoseconds.")
            .customMetric(
                "kafka.consumer.io_time_ns_total",
                MetricInstrument.COUNTER,
                "The total time the I/O thread spent doing I/O.")
            .customMetric(
                "kafka.consumer.io_wait_ratio",
                MetricInstrument.GAUGE,
                "*Deprecated* The fraction of time the I/O thread spent waiting.")
            .customMetric(
                "kafka.consumer.io_wait_time_ns_avg",
                MetricInstrument.GAUGE,
                "The average length of time the I/O thread spent waiting for a socket ready for reads or writes in nanoseconds.")
            .customMetric(
                "kafka.consumer.io_wait_time_ns_total",
                MetricInstrument.COUNTER,
                "The total time the I/O thread spent waiting")
            .customMetric(
                "kafka.consumer.io_waittime_total",
                MetricInstrument.COUNTER,
                "*Deprecated* The total time the I/O thread spent waiting.")
            .customMetric(
                "kafka.consumer.iotime_total",
                MetricInstrument.COUNTER,
                "*Deprecated* The total time the I/O thread spent doing I/O.")
            .customMetric(
                "kafka.consumer.last_poll_seconds_ago",
                MetricInstrument.GAUGE,
                "The number of seconds since the last poll() invocation.")
            .customMetric(
                "kafka.consumer.network_io_rate",
                MetricInstrument.GAUGE,
                "The number of network operations (reads or writes) on all connections per second.")
            .customMetric(
                "kafka.consumer.network_io_total",
                MetricInstrument.COUNTER,
                "The total number of network operations (reads or writes) on all connections.")
            .customMetric(
                "kafka.consumer.poll_idle_ratio_avg",
                MetricInstrument.GAUGE,
                "The average fraction of time the consumer's poll() is idle as opposed to waiting for the user code to process records.")
            .customMetric(
                "kafka.consumer.reauthentication_latency_avg",
                MetricInstrument.GAUGE,
                "The average latency observed due to re-authentication.")
            .customMetric(
                "kafka.consumer.reauthentication_latency_max",
                MetricInstrument.GAUGE,
                "The max latency observed due to re-authentication.")
            .customMetric(
                "kafka.consumer.select_rate",
                MetricInstrument.GAUGE,
                "The number of times the I/O layer checked for new I/O to perform per second.")
            .customMetric(
                "kafka.consumer.select_total",
                MetricInstrument.COUNTER,
                "The total number of times the I/O layer checked for new I/O to perform.")
            .customMetric(
                "kafka.consumer.successful_authentication_no_reauth_total",
                MetricInstrument.COUNTER,
                "The total number of connections with successful authentication where the client does not support re-authentication.")
            .customMetric(
                "kafka.consumer.successful_authentication_rate",
                MetricInstrument.GAUGE,
                "The number of connections with successful authentication per second.")
            .customMetric(
                "kafka.consumer.successful_authentication_total",
                MetricInstrument.COUNTER,
                "The total number of connections with successful authentication.")
            .customMetric(
                "kafka.consumer.successful_reauthentication_rate",
                MetricInstrument.GAUGE,
                "The number of successful re-authentication of connections per second.")
            .customMetric(
                "kafka.consumer.successful_reauthentication_total",
                MetricInstrument.COUNTER,
                "The total number of successful re-authentication of connections.")
            .customMetric(
                "kafka.consumer.time_between_poll_avg",
                MetricInstrument.GAUGE,
                "The average delay between invocations of poll() in milliseconds.")
            .customMetric(
                "kafka.consumer.time_between_poll_max",
                MetricInstrument.GAUGE,
                "The max delay between invocations of poll() in milliseconds.")
            .customMetric(
                "kafka.consumer.incoming_byte_rate",
                MetricInstrument.GAUGE,
                "The number of bytes read off all sockets per second.")
            .customMetric(
                "kafka.consumer.incoming_byte_total",
                MetricInstrument.COUNTER,
                "The total number of bytes read off all sockets.")
            .customMetric(
                "kafka.consumer.outgoing_byte_rate",
                MetricInstrument.GAUGE,
                "The number of outgoing bytes sent to all servers per second.")
            .customMetric(
                "kafka.consumer.outgoing_byte_total",
                MetricInstrument.COUNTER,
                "The total number of outgoing bytes sent to all servers.")
            .customMetric(
                "kafka.consumer.request_latency_avg",
                MetricInstrument.GAUGE,
                "The average request latency in ms.")
            .customMetric(
                "kafka.consumer.request_latency_max",
                MetricInstrument.GAUGE,
                "The maximum request latency in ms.")
            .customMetric(
                "kafka.consumer.request_rate",
                MetricInstrument.GAUGE,
                "The number of requests sent per second.")
            .customMetric(
                "kafka.consumer.request_size_avg",
                MetricInstrument.GAUGE,
                "The average size of requests sent.")
            .customMetric(
                "kafka.consumer.request_size_max",
                MetricInstrument.GAUGE,
                "The maximum size of any request sent.")
            .customMetric(
                "kafka.consumer.request_total",
                MetricInstrument.COUNTER,
                "The total number of requests sent.")
            .customMetric(
                "kafka.consumer.response_rate",
                MetricInstrument.GAUGE,
                "The number of responses received per second.")
            .customMetric(
                "kafka.consumer.response_total",
                MetricInstrument.COUNTER,
                "The total number of responses received.")
            .customMetric(
                "kafka.producer.batch_size_avg",
                MetricInstrument.GAUGE,
                "The average number of bytes sent per partition per-request.")
            .customMetric(
                "kafka.producer.batch_size_max",
                MetricInstrument.GAUGE,
                "The max number of bytes sent per partition per-request.")
            .customMetric(
                "kafka.producer.batch_split_rate",
                MetricInstrument.GAUGE,
                "The average number of batch splits per second.")
            .customMetric(
                "kafka.producer.batch_split_total",
                MetricInstrument.COUNTER,
                "The total number of batch splits.")
            .customMetric(
                "kafka.producer.buffer_available_bytes",
                MetricInstrument.GAUGE,
                "The total amount of buffer memory that is not being used (either unallocated or in the free list).")
            .customMetric(
                "kafka.producer.buffer_exhausted_rate",
                MetricInstrument.GAUGE,
                "The average per-second number of record sends that are dropped due to buffer exhaustion.")
            .customMetric(
                "kafka.producer.buffer_exhausted_total",
                MetricInstrument.COUNTER,
                "The total number of record sends that are dropped due to buffer exhaustion.")
            .customMetric(
                "kafka.producer.buffer_total_bytes",
                MetricInstrument.GAUGE,
                "The maximum amount of buffer memory the client can use (whether or not it is currently used).")
            .customMetric(
                "kafka.producer.bufferpool_wait_ratio",
                MetricInstrument.GAUGE,
                "The fraction of time an appender waits for space allocation.")
            .customMetric(
                "kafka.producer.bufferpool_wait_time_ns_total",
                MetricInstrument.COUNTER,
                "The total time in nanoseconds an appender waits for space allocation.")
            .customMetric(
                "kafka.producer.bufferpool_wait_time_total",
                MetricInstrument.COUNTER,
                "*Deprecated* The total time an appender waits for space allocation.")
            .customMetric(
                "kafka.producer.compression_rate_avg",
                MetricInstrument.GAUGE,
                "The average compression rate of record batches.")
            .customMetric(
                "kafka.producer.connection_close_rate",
                MetricInstrument.GAUGE,
                "The number of connections closed per second.")
            .customMetric(
                "kafka.producer.connection_close_total",
                MetricInstrument.COUNTER,
                "The total number of connections closed.")
            .customMetric(
                "kafka.producer.connection_count",
                MetricInstrument.GAUGE,
                "The current number of active connections.")
            .customMetric(
                "kafka.producer.connection_creation_rate",
                MetricInstrument.GAUGE,
                "The number of new connections established per second.")
            .customMetric(
                "kafka.producer.connection_creation_total",
                MetricInstrument.COUNTER,
                "The total number of new connections established.")
            .customMetric(
                "kafka.producer.failed_authentication_rate",
                MetricInstrument.GAUGE,
                "The number of connections with failed authentication per second.")
            .customMetric(
                "kafka.producer.failed_authentication_total",
                MetricInstrument.COUNTER,
                "The total number of connections with failed authentication.")
            .customMetric(
                "kafka.producer.failed_reauthentication_rate",
                MetricInstrument.GAUGE,
                "The number of failed re-authentication of connections per second.")
            .customMetric(
                "kafka.producer.failed_reauthentication_total",
                MetricInstrument.COUNTER,
                "The total number of failed re-authentication of connections.")
            .customMetric(
                "kafka.producer.flush_time_ns_total",
                MetricInstrument.COUNTER,
                "Total time producer has spent in flush in nanoseconds.")
            .customMetric(
                "kafka.producer.io_ratio",
                MetricInstrument.GAUGE,
                "*Deprecated* The fraction of time the I/O thread spent doing I/O.")
            .customMetric(
                "kafka.producer.io_time_ns_avg",
                MetricInstrument.GAUGE,
                "The average length of time for I/O per select call in nanoseconds.")
            .customMetric(
                "kafka.producer.io_time_ns_total",
                MetricInstrument.COUNTER,
                "The total time the I/O thread spent doing I/O.")
            .customMetric(
                "kafka.producer.io_wait_ratio",
                MetricInstrument.GAUGE,
                "*Deprecated* The fraction of time the I/O thread spent waiting.")
            .customMetric(
                "kafka.producer.io_wait_time_ns_avg",
                MetricInstrument.GAUGE,
                "The average length of time the I/O thread spent waiting for a socket ready for reads or writes in nanoseconds.")
            .customMetric(
                "kafka.producer.io_wait_time_ns_total",
                MetricInstrument.COUNTER,
                "The total time the I/O thread spent waiting.")
            .customMetric(
                "kafka.producer.io_waittime_total",
                MetricInstrument.COUNTER,
                "*Deprecated* The total time the I/O thread spent waiting.")
            .customMetric(
                "kafka.producer.iotime_total",
                MetricInstrument.COUNTER,
                "*Deprecated* The total time the I/O thread spent doing I/O.")
            .customMetric(
                "kafka.producer.metadata_age",
                MetricInstrument.GAUGE,
                "The age in seconds of the current producer metadata being used.")
            .customMetric(
                "kafka.producer.metadata_wait_time_ns_total",
                MetricInstrument.COUNTER,
                "Total time producer has spent waiting on topic metadata in nanoseconds.")
            .customMetric(
                "kafka.producer.network_io_rate",
                MetricInstrument.GAUGE,
                "The number of network operations (reads or writes) on all connections per second.")
            .customMetric(
                "kafka.producer.network_io_total",
                MetricInstrument.COUNTER,
                "The total number of network operations (reads or writes) on all connections.")
            .customMetric(
                "kafka.producer.produce_throttle_time_avg",
                MetricInstrument.GAUGE,
                "The average time in ms a request was throttled by a broker.")
            .customMetric(
                "kafka.producer.produce_throttle_time_max",
                MetricInstrument.GAUGE,
                "The maximum time in ms a request was throttled by a broker.")
            .customMetric(
                "kafka.producer.reauthentication_latency_avg",
                MetricInstrument.GAUGE,
                "The average latency observed due to re-authentication.")
            .customMetric(
                "kafka.producer.reauthentication_latency_max",
                MetricInstrument.GAUGE,
                "The max latency observed due to re-authentication.")
            .customMetric(
                "kafka.producer.record_queue_time_avg",
                MetricInstrument.GAUGE,
                "The average time in ms record batches spent in the send buffer.")
            .customMetric(
                "kafka.producer.record_queue_time_max",
                MetricInstrument.GAUGE,
                "The maximum time in ms record batches spent in the send buffer.")
            .customMetric(
                "kafka.producer.record_size_avg",
                MetricInstrument.GAUGE,
                "The average record size.")
            .customMetric(
                "kafka.producer.record_size_max",
                MetricInstrument.GAUGE,
                "The maximum record size.")
            .customMetric(
                "kafka.producer.records_per_request_avg",
                MetricInstrument.GAUGE,
                "The average number of records per request.")
            .customMetric(
                "kafka.producer.requests_in_flight",
                MetricInstrument.GAUGE,
                "The current number of in-flight requests awaiting a response.")
            .customMetric(
                "kafka.producer.select_rate",
                MetricInstrument.GAUGE,
                "The number of times the I/O layer checked for new I/O to perform per second.")
            .customMetric(
                "kafka.producer.select_total",
                MetricInstrument.COUNTER,
                "The total number of times the I/O layer checked for new I/O to perform.")
            .customMetric(
                "kafka.producer.successful_authentication_no_reauth_total",
                MetricInstrument.COUNTER,
                "The total number of connections with successful authentication where the client does not support re-authentication.")
            .customMetric(
                "kafka.producer.successful_authentication_rate",
                MetricInstrument.GAUGE,
                "The number of connections with successful authentication per second.")
            .customMetric(
                "kafka.producer.successful_authentication_total",
                MetricInstrument.COUNTER,
                "The total number of connections with successful authentication.")
            .customMetric(
                "kafka.producer.successful_reauthentication_rate",
                MetricInstrument.GAUGE,
                "The number of successful re-authentication of connections per second.")
            .customMetric(
                "kafka.producer.successful_reauthentication_total",
                MetricInstrument.COUNTER,
                "The total number of successful re-authentication of connections.")
            .customMetric(
                "kafka.producer.txn_abort_time_ns_total",
                MetricInstrument.COUNTER,
                "Total time producer has spent in abortTransaction in nanoseconds.")
            .customMetric(
                "kafka.producer.txn_begin_time_ns_total",
                MetricInstrument.COUNTER,
                "Total time producer has spent in beginTransaction in nanoseconds.")
            .customMetric(
                "kafka.producer.txn_commit_time_ns_total",
                MetricInstrument.COUNTER,
                "Total time producer has spent in commitTransaction in nanoseconds.")
            .customMetric(
                "kafka.producer.txn_init_time_ns_total",
                MetricInstrument.COUNTER,
                "Total time producer has spent in initTransactions in nanoseconds.")
            .customMetric(
                "kafka.producer.txn_send_offsets_time_ns_total",
                MetricInstrument.COUNTER,
                "Total time producer has spent in sendOffsetsToTransaction in nanoseconds.")
            .customMetric(
                "kafka.producer.waiting_threads",
                MetricInstrument.GAUGE,
                "The number of user threads blocked waiting for buffer memory to enqueue their records.")
            .customMetric(
                "kafka.producer.incoming_byte_rate",
                MetricInstrument.GAUGE,
                "The number of bytes read off all sockets per second.")
            .customMetric(
                "kafka.producer.incoming_byte_total",
                MetricInstrument.COUNTER,
                "The total number of bytes read off all sockets.")
            .customMetric(
                "kafka.producer.outgoing_byte_rate",
                MetricInstrument.GAUGE,
                "The number of outgoing bytes sent to all servers per second.")
            .customMetric(
                "kafka.producer.outgoing_byte_total",
                MetricInstrument.COUNTER,
                "The total number of outgoing bytes sent to all servers.")
            .customMetric(
                "kafka.producer.request_latency_avg",
                MetricInstrument.GAUGE,
                "The average request latency in ms.")
            .customMetric(
                "kafka.producer.request_latency_max",
                MetricInstrument.GAUGE,
                "The maximum request latency in ms.")
            .customMetric(
                "kafka.producer.request_rate",
                MetricInstrument.GAUGE,
                "The number of requests sent per second.")
            .customMetric(
                "kafka.producer.request_size_avg",
                MetricInstrument.GAUGE,
                "The average size of requests sent.")
            .customMetric(
                "kafka.producer.request_size_max",
                MetricInstrument.GAUGE,
                "The maximum size of any request sent.")
            .customMetric(
                "kafka.producer.request_total",
                MetricInstrument.COUNTER,
                "The total number of requests sent.")
            .customMetric(
                "kafka.producer.response_rate",
                MetricInstrument.GAUGE,
                "The number of responses received per second.")
            .customMetric(
                "kafka.producer.response_total",
                MetricInstrument.COUNTER,
                "The total number of responses received.")
            .customMetric(
                "kafka.producer.byte_rate",
                MetricInstrument.GAUGE,
                "The average number of bytes sent per second for a topic.")
            .customMetric(
                "kafka.producer.byte_total",
                MetricInstrument.COUNTER,
                "The total number of bytes sent for a topic.")
            .customMetric(
                "kafka.producer.compression_rate",
                MetricInstrument.GAUGE,
                "The average compression rate of record batches for a topic.")
            .customMetric(
                "kafka.producer.record_error_rate",
                MetricInstrument.GAUGE,
                "The average per-second number of record sends that resulted in errors.")
            .customMetric(
                "kafka.producer.record_error_total",
                MetricInstrument.COUNTER,
                "The total number of record sends that resulted in errors.")
            .customMetric(
                "kafka.producer.record_retry_rate",
                MetricInstrument.GAUGE,
                "The average per-second number of retried record sends.")
            .customMetric(
                "kafka.producer.record_retry_total",
                MetricInstrument.COUNTER,
                "The total number of retried record sends.")
            .customMetric(
                "kafka.producer.record_send_rate",
                MetricInstrument.GAUGE,
                "The average number of records sent per second.")
            .customMetric(
                "kafka.producer.record_send_total",
                MetricInstrument.COUNTER,
                "The total number of records sent.")
            .build());
    instrumentations.add(
        instrumentation("jsf-myfaces").component("Apache MyFaces", "1.2 and higher").build());
    instrumentations.add(
        instrumentation("pekko-actor").component("Apache Pekko Actors", "1.0 and higher").build());
    instrumentations.add(
        instrumentation("pekko-http")
            .component("Apache Pekko HTTP", "1.0 and higher")
            .httpClientMetrics()
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("pulsar")
            .component("Apache Pulsar", "2.8 and higher")
            .messagingPublisherMetrics()
            .messagingConsumerMetrics()
            .build());
    instrumentations.add(
        instrumentation("rocketmq-client")
            .component("Apache RocketMQ gRPC/Protobuf-based Client", "5.0 and higher")
            .component("Apache RocketMQ Remoting-based Client", "4.8 and higher")
            .build());
    instrumentations.add(
        instrumentation("apache-shenyu").component("Apache ShenYu", "2.4 and higher").build());
    instrumentations.add(
        instrumentation("struts").component("Apache Struts 2", "2.3 and higher").build());
    instrumentations.add(
        instrumentation("tomcat")
            .component("Apache Tomcat", "7.0 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("tapestry").component("Apache Tapestry", "5.4 and higher").build());
    instrumentations.add(
        instrumentation("wicket").component("Apache Wicket", "8.0 and higher").build());
    instrumentations.add(
        instrumentation("armeria")
            .component("Armeria", "1.3 and higher")
            .httpClientMetrics()
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("armeria")
            .component("Armeria gRPC", "1.14 and higher")
            .rpcClientMetrics()
            .rpcServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("async-http-client")
            .component("AsyncHttpClient", "1.9 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("avaje-jex").component("Avaje Jex", "3.0 and higher").build());
    instrumentations.add(
        instrumentation("aws-lambda").component("AWS Lambda", "1.0 and higher").build());
    instrumentations.add(
        instrumentation("aws-sdk")
            .component("AWS SDK 1", "1.11 and higher")
            .component("AWS SDK 2", "2.2 and higher")
            .dbClientMetrics()
            .genAiClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("azure-core").component("Azure Core", "1.14 and higher").build());
    instrumentations.add(
        instrumentation("cassandra")
            .component("Cassandra Driver", "3.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("clickhouse-client-v1")
            .component("Clickhouse Client V1", "0.5 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("clickhouse-client-v2")
            .component("Clickhouse Client V2", "0.8 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("couchbase")
            .component("Couchbase Client", "2.0 to 3.0 and 3.1 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("c3p0")
            .component("c3p0", "0.9.2 and higher")
            .dbPoolMetrics(DbPoolMetrics.USAGE, DbPoolMetrics.PENDING_REQUESTS)
            .build());
    instrumentations.add(
        instrumentation("dropwizard-metrics", "Disabled by default")
            .component("Dropwizard Metrics", "4.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("dropwizard-views")
            .component("Dropwizard Views", "0.7 and higher")
            .build());
    instrumentations.add(
        instrumentation("grizzly")
            .component("Eclipse Grizzly", "2.3 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("jersey").component("Eclipse Jersey", "2.0 and higher").build());
    instrumentations.add(
        instrumentation("jetty")
            .component("Eclipse Jetty", "8.0 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("jetty-httpclient")
            .component("Eclipse Jetty HTTP Client", "9.2 to 10.0, 12.0 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("metro").component("Eclipse Metro", "2.2 and higher").build());
    instrumentations.add(
        instrumentation("jsf-mojarra").component("Eclipse Mojarra", "1.2 and higher").build());
    instrumentations.add(
        instrumentation("elasticsearch-api-client")
            .component("Elasticsearch API Client", "7.16 and higher")
            .build());
    instrumentations.add(
        instrumentation("elasticsearch-rest")
            .component("Elasticsearch REST Client", "5.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("elasticsearch-transport")
            .component("Elasticsearch Transport Client", "5.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("finagle-http").component("Finagle", "23.11 and higher").build());
    instrumentations.add(instrumentation("finatra").component("Finatra", "2.9 and higher").build());
    instrumentations.add(
        instrumentation("geode")
            .component("Geode Client", "1.4 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("google-http-client")
            .component("Google HTTP Client", "1.19 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(instrumentation("grails").component("Grails", "3.0 and higher").build());
    instrumentations.add(
        instrumentation("graphql-java").component("GaphQL Java", "12.0 and higher").build());
    instrumentations.add(
        instrumentation("grpc")
            .component("gRPC", "1.6 and higher")
            .rpcClientMetrics()
            .rpcServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("guava").component("Guava ListenableFuture", "0.9.2 and higher").build());
    instrumentations.add(instrumentation("gwt").component("GWT", "0.9.2 and higher").build());
    instrumentations.add(
        instrumentation("hibernate").component("Hibernate", "3.3 and higher").build());
    instrumentations.add(
        instrumentation("hibernate-reactive")
            .component("Hibernate Reactive", "1.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("hikaricp")
            .component("HikariCP", "3.0 and higher")
            .dbPoolMetrics(
                DbPoolMetrics.USAGE,
                DbPoolMetrics.IDLE_MIN,
                DbPoolMetrics.MAX,
                DbPoolMetrics.PENDING_REQUESTS,
                DbPoolMetrics.TIMEOUTS,
                DbPoolMetrics.CREATE_TIME,
                DbPoolMetrics.USE_TIME,
                DbPoolMetrics.WAIT_TIME)
            .build());
    instrumentations.add(
        instrumentation("http-url-connection")
            .component("HttpURLConnection", null)
            .httpClientMetrics()
            .build());
    instrumentations.add(instrumentation("hystrix").component("Hystrix", "1.4 and higher").build());
    instrumentations.add(
        instrumentation("influxdb")
            .component("InfluxDB Client", "2.4 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(instrumentation("executors").component("Java Executors", null).build());
    instrumentations.add(
        instrumentation("java-http-client")
            .component("Java HTTP Client", null)
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("java-http-server")
            .component("Java HTTP Server", null)
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("java-util-logging").component("java.util.logging", null).build());
    instrumentations.add(
        instrumentation("runtime-telemetry")
            .component("Java Platform", null)
            .bundledMetric(
                "jvm.class.loaded",
                MetricInstrument.COUNTER,
                "Number of classes loaded since JVM start.")
            .bundledMetric(
                "jvm.class.unloaded",
                MetricInstrument.COUNTER,
                "Number of classes unloaded since JVM start.")
            .bundledMetric(
                "jvm.class.count",
                MetricInstrument.UP_DOWN_COUNTER,
                "Number of classes currently loaded.")
            .bundledMetric(
                "jvm.cpu.time",
                MetricInstrument.COUNTER,
                "CPU time used by the process as reported by the JVM.")
            .bundledMetric(
                "jvm.cpu.count",
                MetricInstrument.UP_DOWN_COUNTER,
                "Number of processors available to the Java virtual machine.")
            .bundledMetric(
                "jvm.cpu.recent_utilization",
                MetricInstrument.GAUGE,
                "Recent CPU utilization for the process as reported by the JVM.")
            .bundledMetric(
                "jvm.gc.duration",
                MetricInstrument.HISTOGRAM,
                "Duration of JVM garbage collection actions.")
            .bundledMetric(
                "jvm.memory.used", MetricInstrument.UP_DOWN_COUNTER, "Measure of memory used.")
            .bundledMetric(
                "jvm.memory.committed",
                MetricInstrument.UP_DOWN_COUNTER,
                "Measure of memory committed.")
            .bundledMetric(
                "jvm.memory.limit",
                MetricInstrument.UP_DOWN_COUNTER,
                "Measure of max obtainable memory.")
            .bundledMetric(
                "jvm.memory.used_after_last_gc",
                MetricInstrument.UP_DOWN_COUNTER,
                "Measure of memory used, as measured after the most recent garbage collection event on this pool.")
            .bundledMetric(
                "jvm.thread.count",
                MetricInstrument.UP_DOWN_COUNTER,
                "Number of executing platform threads (disabled by default).")
            .customMetric(
                "jvm.buffer.memory.used",
                MetricInstrument.UP_DOWN_COUNTER,
                "Measure of memory used by buffers (disabled by default).")
            .customMetric(
                "jvm.buffer.memory.limit",
                MetricInstrument.UP_DOWN_COUNTER,
                "Measure of total memory capacity of buffers (disabled by default).")
            .customMetric(
                "jvm.buffer.count",
                MetricInstrument.UP_DOWN_COUNTER,
                "Number of buffers in the pool (disabled by default).")
            .customMetric(
                "jvm.system.cpu.load_1m",
                MetricInstrument.GAUGE,
                "Average CPU load of the whole system for the last minute as reported by the JVM (disabled by default).")
            .customMetric(
                "jvm.system.cpu.utilization",
                MetricInstrument.GAUGE,
                "Recent CPU utilization for the whole system as reported by the JVM (disabled by default).")
            .customMetric(
                "jvm.memory.init",
                MetricInstrument.UP_DOWN_COUNTER,
                "Measure of initial memory requested (disabled by default).")
            .customMetric(
                "jvm.file_descriptor.count",
                MetricInstrument.UP_DOWN_COUNTER,
                "Number of open file descriptors as reported by the JVM (disabled by default).")
            // XXX JFR metrics from runtime-telemetry-java17 are missing
            .build());
    instrumentations.add(instrumentation("javalin").component("Javalin", "5.0 and higher").build());
    instrumentations.add(instrumentation("jaxrs").component("JAX-RS", "0.5 and higher").build());
    instrumentations.add(
        instrumentation("jaxrs-client")
            .component("JAX-RS Client", "1.1 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(instrumentation("jaxws").component("JAX-WS", "2.0 to 3.0").build());
    instrumentations.add(
        instrumentation(List.of("jboss-logmanager-appender", "jboss-logmanager-mdc"))
            .component("JBoss Log Manager", "1.1 and higher")
            .build());
    instrumentations.add(
        instrumentation(List.of("jdbc", "jdbc-datasource"))
            .component("JDBC", null)
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("jedis").component("Jedis", "1.4 and higher").dbClientMetrics().build());
    instrumentations.add(instrumentation("jms").component("JMS", "1.1 and higher").build());
    instrumentations.add(
        instrumentation("jodd-http")
            .component("Jodd HTTP", "4.2 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(instrumentation("jsp").component("JSP", "2.3 and higher").build());
    instrumentations.add(
        instrumentation("kotlinx-coroutines")
            .component("Kotlin Coroutines", "1.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("ktor")
            .component("Ktor", "2.0 and higher")
            .httpClientMetrics()
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("kubernetes-client")
            .component("Kubernetes Client", "7.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("lettuce")
            .component("Lettuce", "4.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("liberty").component("Liberty", "20.0 and higher").build());
    instrumentations.add(
        instrumentation(List.of("log4j-appender", "log4j-mdc", "log4j-context-data"))
            .component("Log4j", "1.2 and higher")
            .build());
    instrumentations.add(
        instrumentation(List.of("logback-appender", "logback-mdc"))
            .component("Logback", "1.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("micrometer").component("Micrometer", "1.5 and higher").build());
    instrumentations.add(
        instrumentation("mongo")
            .component("MongoDB Drive", "3.1 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(instrumentation("mybatis").component("MyBatis", "3.2 and higher").build());
    instrumentations.add(
        instrumentation("netty")
            .component("Netty", "3.8 and higher")
            .httpClientMetrics()
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("okhttp")
            .component("OkHttp", "2.2 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("liberty")
            .component("OpenLiberty", "20.0 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("opensearch")
            .component("OpenSearch REST Client", "1.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("openai-java")
            .component("OpenAI Java SDK", "1.1 and higher")
            .genAiClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("opentelemetry-api").component("OpenTelemetry API", null).build());
    instrumentations.add(
        instrumentation("opentelemetry-extension-annotations")
            .component("OpenTelemetry Extension Annotations", null)
            .build());
    instrumentations.add(
        instrumentation("opentelemetry-instrumentation-annotations")
            .component("OpenTelemetry Instrumentation Annotations", null)
            .build());
    instrumentations.add(
        instrumentation("oracle-ucp")
            .component("Oracle UCP", "11.2 and higher")
            .dbPoolMetrics(DbPoolMetrics.USAGE, DbPoolMetrics.MAX, DbPoolMetrics.PENDING_REQUESTS)
            .build());
    instrumentations.add(instrumentation("oshi").component("OSHI", "5.3.1 and higher").build());
    instrumentations.add(instrumentation("payara").component("Payara", "5.0 and higher").build());
    instrumentations.add(instrumentation("play-mvc").component("Play", "2.4 and higher").build());
    instrumentations.add(
        instrumentation("play-ws")
            .component("Play WS", "1.0 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("powerjob").component("PowerJob", "4.0 and higher").build());
    instrumentations.add(
        instrumentation("quarkus-resteasy-reactive")
            .component("Quarkus Resteasy Reactive", "2.16.7 and higher")
            .build());
    instrumentations.add(instrumentation("quartz").component("Quartz", "2.0 and higher").build());
    instrumentations.add(
        instrumentation("r2dbc").component("R2DBC", "1.0 and higher").dbClientMetrics().build());
    instrumentations.add(
        instrumentation("rabbitmq").component("RabbitMQ Client", "2.7 and higher").build());
    instrumentations.add(
        instrumentation("ratpack")
            .component("Ratpack", "1.4 and higher")
            .httpClientMetrics()
            .httpServerMetrics()
            .build());
    instrumentations.add(instrumentation("reactor").component("Reactor", "3.1 and higher").build());
    instrumentations.add(
        instrumentation("reactor-netty")
            .component("Reactor Netty", "0.9 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("rediscala")
            .component("Rediscala", "1.8 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("redisson")
            .component("Redisson", "3.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("resteasy").component("RESTEasy", "3.0 and higher").build());
    instrumentations.add(
        instrumentation("restlet")
            .component("Restlet", "1.0 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(instrumentation("rmi").component("RMI", null).build());
    instrumentations.add(instrumentation("rxjava").component("RxJava", "2.2 and higher").build());
    instrumentations.add(
        instrumentation("scala-fork-join")
            .component("Scala ForkJoinPool", "2.8 and higher")
            .build());
    instrumentations.add(
        instrumentation("servlet")
            .component("Servlet", "2.2 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(
        instrumentation("spark").component("Spark Web Framework", "2.3 and higher").build());
    instrumentations.add(
        instrumentation("spring-batch", "Disabled by default")
            .component("Spring Batch", "2.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("spring-cloud-gateway")
            .component("Spring Cloud AWS", "3.0 and higher")
            .build());
    instrumentations.add(
        instrumentation("spring-cloud-gateway")
            .component("Spring Cloud Gateway", "1.8 and higher")
            .build());
    instrumentations.add(
        instrumentation("spring-core").component("Spring Core", "2.0 and higher").build());
    instrumentations.add(
        instrumentation("spring-data").component("Spring Data", "1.8 and higher").build());
    instrumentations.add(
        instrumentation("spring-integration")
            .component("Spring Integration", "4.1 to 6.0")
            .build());
    instrumentations.add(
        instrumentation("spring-jms").component("Spring JMS", "2.0 and higher").build());
    instrumentations.add(
        instrumentation("spring-kafka").component("Spring Kafka", "2.7 and higher").build());
    instrumentations.add(
        instrumentation("spring-pulsar").component("Spring Pulsar", "1.0 and higher").build());
    instrumentations.add(
        instrumentation("spring-rabbit").component("Spring RabbitMQ", "1.0 and higher").build());
    instrumentations.add(
        instrumentation("spring-web")
            .component("Spring RestTemplate", "3.1 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("spring-rmi").component("Spring RMI", "4.0 and higher").build());
    instrumentations.add(
        instrumentation("spring-scheduling")
            .component("Spring Scheduling", "3.1 and higher")
            .build());
    instrumentations.add(
        instrumentation("spring-webmvc").component("Spring Web MVC", "3.1 and higher").build());
    instrumentations.add(
        instrumentation("spring-ws").component("Spring Web Service", "2.0 and higher").build());
    instrumentations.add(
        instrumentation("spring-webflux")
            .component("Spring WebFlux", "5.3 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("spymemcached")
            .component("Spymemcached", "2.12 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("tomcat-jdbc")
            .component("Tomcat JDBC", "8.5 and higher")
            .dbPoolMetrics(
                DbPoolMetrics.USAGE,
                DbPoolMetrics.IDLE_MAX,
                DbPoolMetrics.IDLE_MIN,
                DbPoolMetrics.MAX,
                DbPoolMetrics.PENDING_REQUESTS)
            .build());
    instrumentations.add(instrumentation("twilio").component("Twilio", "6.6 to 8.0").build());
    instrumentations.add(
        instrumentation("undertow")
            .component("Undertow", "1.4 and higher")
            .httpServerMetrics()
            .build());
    instrumentations.add(instrumentation("vaadin").component("Vaadin", "14.2 and higher").build());
    instrumentations.add(
        instrumentation("vertx-http-client")
            .component("Vert.x HttpClient", "3.0 and higher")
            .httpClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("vertx-kafka-client")
            .component("Vert.x Kafka Client", "3.6 and higher")
            .build());
    instrumentations.add(
        instrumentation("vertx-redis-client")
            .component("Vert.x Redis Client", "3.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("vertx-rx-java").component("Vert.x RxJava2", "3.5 and higher").build());
    instrumentations.add(
        instrumentation("vertx-sql-client")
            .component("Vert.x SQL Client", "4.0 and higher")
            .dbClientMetrics()
            .build());
    instrumentations.add(
        instrumentation("vertx-web").component("Vert.x Web", "3.0 and higher").build());
    instrumentations.add(
        instrumentation("vibur-dbcp")
            .component("Vibur DBCP", "11.0 and higher")
            .dbPoolMetrics(DbPoolMetrics.USAGE, DbPoolMetrics.MAX)
            .build());
    instrumentations.add(
        instrumentation("xxl-job").component("XXL-JOB", "1.9.2 and higher").build());
    instrumentations.add(instrumentation("zio").component("ZIO", "2.0 and higher").build());

    // splunk instrumentations
    instrumentations.add(
        splunkInstrumentation("jvm-metrics.splunk")
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
        splunkInstrumentation("khttp").component("khttp", "0.1 and higher").build());
    instrumentations.add(
        splunkInstrumentation("glassfish").component("GlassFish", "5.0 and higher").build());
    // XXX jetty, liberty and tomcat have the same key as an existing otel instrumentation
    instrumentations.add(
        splunkInstrumentation("jetty").component("Jetty", "9.4 and higher").build());
    instrumentations.add(
        splunkInstrumentation("liberty").component("Liberty", "20.0 and higher").build());
    instrumentations.add(
        splunkInstrumentation(List.of("tomcat", "tomcat-metrics-splunk"))
            .component("Tomcat", "7.0 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("tomee").component("TomEE", "7.0 and higher").build());
    instrumentations.add(
        splunkInstrumentation(List.of("weblogic", "weblogic-metrics-splunk"))
            .component("WebLogic", "12.1 and higher")
            .build());
    instrumentations.add(
        splunkInstrumentation("websphere").component("WebSphere", "8.5.5 and higher").build());
    instrumentations.add(
        splunkInstrumentation("wildfly").component("WildFly", "13.0 and higher").build());

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
                List.of("os.type", "os.description"))
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

  static InstrumentationBuilder instrumentation(String key) {
    return instrumentation(key, null);
  }

  static InstrumentationBuilder instrumentation(String key, String description) {
    return instrumentation(key, description, Stability.EXPERIMENTAL, Support.COMMUNITY);
  }

  static InstrumentationBuilder instrumentation(List<String> keys) {
    return instrumentation(keys, null);
  }

  static InstrumentationBuilder instrumentation(List<String> keys, String description) {
    return instrumentation(keys, description, Stability.EXPERIMENTAL, Support.COMMUNITY);
  }

  static InstrumentationBuilder instrumentation(
      String key, String description, Stability stability, Support support) {
    return instrumentation(Collections.singletonList(key), description, stability, support);
  }

  static InstrumentationBuilder instrumentation(
      List<String> keys, String description, Stability stability, Support support) {
    return new InstrumentationBuilder(keys, description, stability, support);
  }

  static InstrumentationBuilder splunkInstrumentation(String key) {
    return splunkInstrumentation(key, null);
  }

  static InstrumentationBuilder splunkInstrumentation(String key, String description) {
    return instrumentation(key, description, Stability.EXPERIMENTAL, Support.SUPPORTED);
  }

  static InstrumentationBuilder splunkInstrumentation(List<String> keys) {
    return splunkInstrumentation(keys, null);
  }

  static InstrumentationBuilder splunkInstrumentation(List<String> keys, String description) {
    return instrumentation(keys, description, Stability.EXPERIMENTAL, Support.SUPPORTED);
  }

  private static class InstrumentationBuilder {
    private final List<String> keys;
    private final String description;
    private final Stability stability;
    private final Support support;
    private final List<Object> instrumentedComponents = new ArrayList<>();
    private final List<Object> dependencies = new ArrayList<>();
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

    InstrumentationBuilder httpServerMetrics() {
      bundledMetric(
          "http.server.request.duration",
          MetricInstrument.HISTOGRAM,
          "Duration of HTTP server requests.");
      customMetric(
          "http.server.active_requests",
          MetricInstrument.UP_DOWN_COUNTER,
          "Number of active HTTP server requests (disabled by default).");
      customMetric(
          "http.server.request.body.size",
          MetricInstrument.HISTOGRAM,
          "Size of HTTP server request bodies (disabled by default).");
      customMetric(
          "http.server.response.body.size",
          MetricInstrument.HISTOGRAM,
          "Size of HTTP server response bodies (disabled by default).");

      return this;
    }

    InstrumentationBuilder rpcClientMetrics() {
      customMetric(
          "rpc.client.duration",
          MetricInstrument.HISTOGRAM,
          "The duration of an outbound RPC invocation.");

      return this;
    }

    InstrumentationBuilder rpcServerMetrics() {
      customMetric(
          "rpc.server.duration",
          MetricInstrument.HISTOGRAM,
          "The duration of an inbound RPC invocation.");

      return this;
    }

    InstrumentationBuilder messagingPublisherMetrics() {
      customMetric(
          "messaging.publish.duration",
          MetricInstrument.HISTOGRAM,
          "Measures the duration of publish operation.");

      return this;
    }

    InstrumentationBuilder messagingConsumerMetrics() {
      customMetric(
          "messaging.receive.duration",
          MetricInstrument.HISTOGRAM,
          "Measures the duration of receive operation.");
      customMetric(
          "messaging.receive.messages",
          MetricInstrument.COUNTER,
          "Measures the number of received messages.");

      return this;
    }

    InstrumentationBuilder dbPoolMetrics(DbPoolMetrics... dbPoolMetrics) {
      for (DbPoolMetrics metric : dbPoolMetrics) {
        metric.add(this);
      }

      return this;
    }

    InstrumentationBuilder dbClientMetrics() {
      // only with stable semconv opt-in
      customMetric(
          "db.client.operation.duration",
          MetricInstrument.HISTOGRAM,
          "Duration of database client operations.");

      return this;
    }

    InstrumentationBuilder genAiClientMetrics() {
      customMetric(
          "gen_ai.client.token.usage",
          MetricInstrument.HISTOGRAM,
          "Measures number of input and output tokens used.");
      customMetric(
          "gen_ai.client.operation.duration",
          MetricInstrument.HISTOGRAM,
          "GenAI operation duration.");

      return this;
    }

    InstrumentationBuilder dependency(
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
      if (!dependencies.isEmpty()) {
        map.put("dependencies", dependencies);
      }
      if (!signals.isEmpty()) {
        map.put("signals", signals);
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
    STRING;

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

  enum DbPoolMetrics {
    // renamed to db.client.connection.count in stable semconv
    USAGE(
        "db.client.connections.usage",
        MetricInstrument.UP_DOWN_COUNTER,
        "The number of connections that are currently in state described by the state attribute."),
    IDLE_MAX(
        "db.client.connections.idle.max",
        MetricInstrument.UP_DOWN_COUNTER,
        "The maximum number of idle open connections allowed."),
    IDLE_MIN(
        "db.client.connections.idle.min",
        MetricInstrument.UP_DOWN_COUNTER,
        "The minimum number of idle open connections allowed."),
    MAX(
        "db.client.connections.max",
        MetricInstrument.UP_DOWN_COUNTER,
        "The maximum number of open connections allowed."),
    PENDING_REQUESTS(
        "db.client.connections.pending_requests",
        MetricInstrument.UP_DOWN_COUNTER,
        "The number of pending requests for an open connection, cumulative for the entire pool."),
    TIMEOUTS(
        "db.client.connections.timeouts",
        MetricInstrument.COUNTER,
        "The number of connection timeouts that have occurred trying to obtain a connection from the pool."),
    CREATE_TIME(
        "db.client.connections.create_time",
        MetricInstrument.HISTOGRAM,
        "The time it took to create a new connection."),
    WAIT_TIME(
        "db.client.connections.wait_time",
        MetricInstrument.HISTOGRAM,
        "The time it took to obtain an open connection from the pool."),
    USE_TIME(
        "db.client.connections.use_time",
        MetricInstrument.HISTOGRAM,
        "The time between borrowing a connection and returning it to the pool.");

    private final String name;
    private final MetricInstrument instrument;
    private final String description;

    DbPoolMetrics(String name, MetricInstrument instrument, String description) {
      this.name = name;
      this.instrument = instrument;
      this.description = description;
    }

    public void add(InstrumentationBuilder builder) {
      builder.customMetric(name, instrument, description);
    }
  }
}
