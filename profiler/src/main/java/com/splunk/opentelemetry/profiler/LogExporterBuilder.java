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
import com.splunk.opentelemetry.profiler.util.DeclarativeConfigPropertiesUtil;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil;
import io.opentelemetry.exporter.otlp.internal.OtlpGrpcLogRecordExporterComponentProvider;
import io.opentelemetry.exporter.otlp.internal.OtlpHttpLogRecordExporterComponentProvider;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ExtendedDeclarativeConfigProperties;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;

class LogExporterBuilder {

  static final String EXTRA_CONTENT_TYPE = "Extra-Content-Type";
  static final String STACKTRACES_HEADER_VALUE = "otel-profiling-stacktraces";
  private static final String DEFAULT_HTTP_LOG_ENDPOINT = "http://localhost:4318/v1/logs";

  static LogRecordExporter fromDeclarativeConfig(
      DeclarativeConfigProperties exporterConfigProperties) {
    if (exporterConfigProperties != null) {
      Set<String> propertyKeys = exporterConfigProperties.getPropertyKeys();
      if (propertyKeys.isEmpty()) {
        DeclarativeConfigProperties otlpHttp = defaultOtlpHttpConfig(exporterConfigProperties);
        OtlpHttpLogRecordExporterComponentProvider provider =
            new OtlpHttpLogRecordExporterComponentProvider();
        return provider.create(toExtended(otlpHttp));
      }

      if (propertyKeys.contains("otlp_log_http")) {
        DeclarativeConfigProperties otlpHttp =
            DeclarativeConfigPropertiesUtil.getStructuredOrEmpty(
                exporterConfigProperties, "otlp_log_http");
        OtlpHttpLogRecordExporterComponentProvider provider =
            new OtlpHttpLogRecordExporterComponentProvider();
        return provider.create(toExtended(otlpHttp));
      }

      if (propertyKeys.contains("otlp_log_grpc")) {
        DeclarativeConfigProperties otlpGrpc =
            DeclarativeConfigPropertiesUtil.getStructuredOrEmpty(
                exporterConfigProperties, "otlp_log_grpc");
        OtlpGrpcLogRecordExporterComponentProvider provider =
            new OtlpGrpcLogRecordExporterComponentProvider();
        return provider.create(toExtended(otlpGrpc));
      }
    }

    throw new ConfigurationException("Profiler exporter configuration is invalid");
  }

  private static DeclarativeConfigProperties defaultOtlpHttpConfig(
      DeclarativeConfigProperties exporterConfigProperties) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("endpoint", DEFAULT_HTTP_LOG_ENDPOINT);
    properties.put("encoding", "protobuf");
    return DeclarativeConfigPropertiesUtil.createPreservingLoader(
        exporterConfigProperties, properties);
  }

  static LogRecordExporter fromEnvironmentConfig() {
    ProfilerConfiguration config = ProfilerConfiguration.SUPPLIER.get();
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
      ProfilerConfiguration config, Supplier<OtlpGrpcLogRecordExporterBuilder> makeBuilder) {
    OtlpGrpcLogRecordExporterBuilder builder = makeBuilder.get();
    String ingestUrl = config.getIngestUrl();
    builder.setEndpoint(ingestUrl);

    return builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE).build();
  }

  @VisibleForTesting
  static LogRecordExporter buildHttpExporter(
      ProfilerConfiguration config, Supplier<OtlpHttpLogRecordExporterBuilder> makeBuilder) {
    OtlpHttpLogRecordExporterBuilder builder = makeBuilder.get();
    String ingestUrl = config.getIngestUrl();

    OtlpConfigUtil.configureOtlpExporterBuilder(
        DATA_TYPE_LOGS,
        (ConfigProperties) config.getConfigProperties(),
        builder::setComponentLoader,
        builder::setEndpoint,
        builder::addHeader,
        builder::setCompression,
        builder::setTimeout,
        builder::setTrustedCertificates,
        builder::setClientTls,
        builder::setRetryPolicy,
        builder::setMemoryMode,
        builder::setInternalTelemetryVersion);

    builder.setEndpoint(ingestUrl);

    return builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE).build();
  }

  public static ExtendedDeclarativeConfigProperties toExtended(
      DeclarativeConfigProperties properties) {
    if (properties instanceof ExtendedDeclarativeConfigProperties) {
      return (ExtendedDeclarativeConfigProperties) properties;
    }
    return new ExtendedDeclarativeConfigPropertiesImpl(properties, ConfigProvider.noop());
  }

  // TODO: This class is copied from the upstream. It should be removed if upstream implementation
  //       is made public, or upstream changes makes it obsolete
  private static class ExtendedDeclarativeConfigPropertiesImpl
      implements ExtendedDeclarativeConfigProperties {

    private final DeclarativeConfigProperties delegate;
    private final ConfigProvider configProvider;

    ExtendedDeclarativeConfigPropertiesImpl(
        DeclarativeConfigProperties delegate, ConfigProvider configProvider) {
      this.delegate = delegate;
      this.configProvider = configProvider;
    }

    @Override
    public ConfigProvider getConfigProvider() {
      return configProvider;
    }

    @Nullable
    @Override
    public String getString(String name) {
      return delegate.getString(name);
    }

    @Nullable
    @Override
    public Boolean getBoolean(String name) {
      return delegate.getBoolean(name);
    }

    @Nullable
    @Override
    public Integer getInt(String name) {
      return delegate.getInt(name);
    }

    @Nullable
    @Override
    public Long getLong(String name) {
      return delegate.getLong(name);
    }

    @Nullable
    @Override
    public Double getDouble(String name) {
      return delegate.getDouble(name);
    }

    @Nullable
    @Override
    public <T> List<T> getScalarList(String name, Class<T> scalarType) {
      return delegate.getScalarList(name, scalarType);
    }

    @Nullable
    @Override
    public DeclarativeConfigProperties getStructured(String name) {
      return delegate.getStructured(name);
    }

    @Nullable
    @Override
    public List<DeclarativeConfigProperties> getStructuredList(String name) {
      return delegate.getStructuredList(name);
    }

    @Override
    public Set<String> getPropertyKeys() {
      return delegate.getPropertyKeys();
    }

    @Override
    public ComponentLoader getComponentLoader() {
      return delegate.getComponentLoader();
    }

    @Override
    public String toString() {
      return "ExtendedDeclarativeConfigPropertiesImpl{" + delegate + '}';
    }
  }
}
