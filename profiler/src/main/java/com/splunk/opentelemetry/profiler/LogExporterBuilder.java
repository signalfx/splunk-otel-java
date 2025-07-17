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
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.function.Supplier;

class LogExporterBuilder {

  static final String EXTRA_CONTENT_TYPE = "Extra-Content-Type";
  static final String STACKTRACES_HEADER_VALUE = "otel-profiling-stacktraces";

  static LogRecordExporter fromConfig(ConfigProperties config) {
    String protocol = Configuration.getOtlpProtocol(config);
    if ("http/protobuf".equals(protocol)) {
      return buildHttpExporter(config, OtlpHttpLogRecordExporter::builder);
    } else if ("grpc".equals(protocol)) {
      return buildGrpcExporter(config, OtlpGrpcLogRecordExporter::builder);
    }
    throw new IllegalStateException("Unsupported OTLP protocol: " + protocol);
  }

  @VisibleForTesting
  static LogRecordExporter buildGrpcExporter(
      ConfigProperties config, Supplier<OtlpGrpcLogRecordExporterBuilder> makeBuilder) {
    OtlpGrpcLogRecordExporterBuilder builder = makeBuilder.get();
    String ingestUrl = Configuration.getConfigUrl(config);
    if (ingestUrl != null) {
      builder.setEndpoint(ingestUrl);
    }
    return builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE).build();
  }

  @VisibleForTesting
  static LogRecordExporter buildHttpExporter(
      ConfigProperties config, Supplier<OtlpHttpLogRecordExporterBuilder> makeBuilder) {
    OtlpHttpLogRecordExporterBuilder builder = makeBuilder.get();
    String ingestUrl = Configuration.getConfigUrl(config);

    OtlpConfigUtil.configureOtlpExporterBuilder(
        DATA_TYPE_LOGS,
        config,
        builder::setComponentLoader,
        builder::setEndpoint,
        builder::addHeader,
        builder::setCompression,
        builder::setTimeout,
        builder::setTrustedCertificates,
        builder::setClientTls,
        builder::setRetryPolicy,
        builder::setMemoryMode);

    if (ingestUrl != null) {
      builder.setEndpoint(ingestUrl);
    }
    return builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE).build();
  }
}
