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

package com.splunk.opentelemetry.opamp;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerDeclarativeConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingDeclarativeConfiguration;
import io.opentelemetry.sdk.declarativeconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.MetricReaderModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class DeclarativeEffectiveConfigFileFactory implements EffectiveConfigFactory {
  private static final String GRPC_DEFAULT_ENDPOINT = "http://localhost:4317";

  DeclarativeEffectiveConfigFileFactory() {}

  public String buildFileContent() {
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigurationInterceptor.getConfigurationModel();
    if (model == null) {
      return "";
    }

    return processModel(model);
  }

  @VisibleForTesting
  String processModel(OpenTelemetryConfigurationModel model) {
    EffectiveConfigBuilder builder = new EffectiveConfigBuilder();
    ProfilerConfiguration profilerConfiguration = ProfilerDeclarativeConfiguration.SUPPLIER.get();
    SnapshotProfilingConfiguration snapshotConfiguration =
        SnapshotProfilingDeclarativeConfiguration.SUPPLIER.get();

    addSplunkEnvVars(builder, profilerConfiguration, snapshotConfiguration);
    addOtelVars(builder, model);

    return builder.build();
  }

  @VisibleForTesting
  void addOtelVars(
      EffectiveConfigBuilder builder, OpenTelemetryConfigurationModel configurationModel) {
    builder
        .add(
            OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS,
            getTracesEndpoints(configurationModel.getTracerProvider()))
        .add(
            OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS,
            getMetricsEndpoints(configurationModel.getMeterProvider()))
        .add(
            OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS,
            getLogsEndpoints(configurationModel.getLoggerProvider()));
  }

  private List<String> getTracesEndpoints(TracerProviderModel tracerProvider) {
    List<String> endpoints = new ArrayList<>();
    if (tracerProvider != null && tracerProvider.getProcessors() != null) {
      for (SpanProcessorModel spanProcessor : tracerProvider.getProcessors()) {
        String endpoint = getEndpoint(getSpanExporter(spanProcessor));
        if (endpoint != null) {
          endpoints.add(endpoint);
        }
      }
    }
    return endpoints;
  }

  private List<String> getMetricsEndpoints(MeterProviderModel meterProvider) {
    List<String> endpoints = new ArrayList<>();
    if (meterProvider != null && meterProvider.getReaders() != null) {
      for (MetricReaderModel metricReader : meterProvider.getReaders()) {
        String endpoint = getEndpoint(getMetricExporter(metricReader));
        if (endpoint != null) {
          endpoints.add(endpoint);
        }
      }
    }
    return endpoints;
  }

  private List<String> getLogsEndpoints(LoggerProviderModel loggerProvider) {
    List<String> endpoints = new ArrayList<>();
    if (loggerProvider != null && loggerProvider.getProcessors() != null) {
      for (LogRecordProcessorModel processor : loggerProvider.getProcessors()) {
        String endpoint = getEndpoint(getLogRecordExporter(processor));
        if (endpoint != null) {
          endpoints.add(endpoint);
        }
      }
    }
    return endpoints;
  }

  private static SpanExporterModel getSpanExporter(SpanProcessorModel processor) {
    if (processor.getBatch() != null) {
      return processor.getBatch().getExporter();
    }
    if (processor.getSimple() != null) {
      return processor.getSimple().getExporter();
    }
    return null;
  }

  private static PushMetricExporterModel getMetricExporter(MetricReaderModel metricReader) {
    if (metricReader.getPeriodic() != null) {
      return metricReader.getPeriodic().getExporter();
    }
    return null;
  }

  private static LogRecordExporterModel getLogRecordExporter(LogRecordProcessorModel processor) {
    if (processor.getBatch() != null) {
      return processor.getBatch().getExporter();
    }
    if (processor.getSimple() != null) {
      return processor.getSimple().getExporter();
    }
    return null;
  }

  private static String getEndpoint(SpanExporterModel exporter) {
    if (exporter == null) {
      return null;
    }
    if (exporter.getOtlpHttp() != null) {
      return Optional.ofNullable(exporter.getOtlpHttp().getEndpoint())
          .orElse("http://localhost:4318/v1/traces");
    } else if (exporter.getOtlpGrpc() != null) {
      return Optional.ofNullable(exporter.getOtlpGrpc().getEndpoint())
          .orElse(GRPC_DEFAULT_ENDPOINT);
    }
    return null;
  }

  private static String getEndpoint(PushMetricExporterModel exporter) {
    if (exporter == null) {
      return null;
    }
    if (exporter.getOtlpHttp() != null) {
      return Optional.ofNullable(exporter.getOtlpHttp().getEndpoint())
          .orElse("http://localhost:4318/v1/metrics");
    } else if (exporter.getOtlpGrpc() != null) {
      return Optional.ofNullable(exporter.getOtlpGrpc().getEndpoint())
          .orElse(GRPC_DEFAULT_ENDPOINT);
    }
    return null;
  }

  private static String getEndpoint(LogRecordExporterModel exporter) {
    if (exporter == null) {
      return null;
    }
    if (exporter.getOtlpHttp() != null) {
      return Optional.ofNullable(exporter.getOtlpHttp().getEndpoint())
          .orElse("http://localhost:4318/v1/logs");
    } else if (exporter.getOtlpGrpc() != null) {
      return Optional.ofNullable(exporter.getOtlpGrpc().getEndpoint())
          .orElse(GRPC_DEFAULT_ENDPOINT);
    }

    return null;
  }
}
