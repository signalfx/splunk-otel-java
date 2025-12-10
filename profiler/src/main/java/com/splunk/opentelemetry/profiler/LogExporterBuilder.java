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

package com.splunk.opentelemetry.profiler;

import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.DATA_TYPE_LOGS;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil;
import io.opentelemetry.exporter.otlp.internal.OtlpHttpLogRecordExporterComponentProvider;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.function.Supplier;

class LogExporterBuilder {

  static final String EXTRA_CONTENT_TYPE = "Extra-Content-Type";
  static final String STACKTRACES_HEADER_VALUE = "otel-profiling-stacktraces";

  static LogRecordExporter fromConfig(DeclarativeConfigProperties exporterConfigProperties) {
    if (exporterConfigProperties != null) {

      DeclarativeConfigProperties otlpHttp = exporterConfigProperties.getStructured("otlp_http");
      if (otlpHttp != null) {
        OtlpHttpLogRecordExporterComponentProvider provider =
            new OtlpHttpLogRecordExporterComponentProvider();
        return provider.create(otlpHttp);
      }

      DeclarativeConfigProperties otlpGrpc = exporterConfigProperties.getStructured("otlp_grpc");
      if (otlpGrpc != null) {
        OtlpHttpLogRecordExporterComponentProvider provider =
            new OtlpHttpLogRecordExporterComponentProvider();
        return provider.create(otlpGrpc);
      }
    }

    throw new ConfigurationException("Profiler exporter configuration is invalid");
  }

  static LogRecordExporter fromConfig(ConfigProperties config) {
    return fromConfig(new ProfilerEnvVarsConfiguration(config));
  }

  static LogRecordExporter fromConfig(ProfilerEnvVarsConfiguration config) {
    String protocol = config.getOtlpProtocol();
    if ("http/protobuf".equals(protocol)) {
      return buildHttpExporter(config, OtlpHttpLogRecordExporter::builder);
    } else if ("grpc".equals(protocol)) {
      return buildGrpcExporter(config, OtlpGrpcLogRecordExporter::builder);
    }
    throw new IllegalStateException("Unsupported OTLP protocol: " + protocol);
  }

  @VisibleForTesting
  static LogRecordExporter buildGrpcExporter(
      ProfilerEnvVarsConfiguration config, Supplier<OtlpGrpcLogRecordExporterBuilder> makeBuilder) {
    OtlpGrpcLogRecordExporterBuilder builder = makeBuilder.get();
    String ingestUrl = config.getIngestUrl();
    builder.setEndpoint(ingestUrl);

    return builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE).build();
  }

  @VisibleForTesting
  static LogRecordExporter buildHttpExporter(
      ProfilerEnvVarsConfiguration config, Supplier<OtlpHttpLogRecordExporterBuilder> makeBuilder) {
    OtlpHttpLogRecordExporterBuilder builder = makeBuilder.get();
    String ingestUrl = config.getIngestUrl();

    OtlpConfigUtil.configureOtlpExporterBuilder(
        DATA_TYPE_LOGS,
        config.getConfigProperties(),
        builder::setComponentLoader,
        builder::setEndpoint,
        builder::addHeader,
        builder::setCompression,
        builder::setTimeout,
        builder::setTrustedCertificates,
        builder::setClientTls,
        builder::setRetryPolicy,
        builder::setMemoryMode);

    builder.setEndpoint(ingestUrl);

    return builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE).build();
  }
}
