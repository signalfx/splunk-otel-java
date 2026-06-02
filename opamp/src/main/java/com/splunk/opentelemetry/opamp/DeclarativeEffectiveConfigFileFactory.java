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
import io.opentelemetry.sdk.declarativeconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.declarativeconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;

class DeclarativeEffectiveConfigFileFactory implements EffectiveConfigFactory {
  private static final String GRPC_DEFAULT_ENDPOINT = "http://localhost:4317";

  DeclarativeEffectiveConfigFileFactory() {}

  @Override
  public String getContentType() {
    return "application/yaml; vendor=splunk; v=1.0.0";
  }

  @Override
  public String getFileName() {
    String fileName = getProperty("otel.config.file");
    if (fileName == null || fileName.isEmpty()) {
      fileName = getProperty("otel.experimental.config.file");
    }
    return fileName;
  }

  public String createEffectiveConfigContent() {
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigurationInterceptor.getConfigurationModel();
    if (model == null) {
      return "";
    }
    ProfilerConfiguration profilerConfiguration = ProfilerDeclarativeConfiguration.SUPPLIER.get();
    SnapshotProfilingConfiguration snapshotConfiguration =
        SnapshotProfilingDeclarativeConfiguration.SUPPLIER.get();

    return processModel(model, profilerConfiguration, snapshotConfiguration);
  }

  @VisibleForTesting
  String processModel(
      OpenTelemetryConfigurationModel model,
      ProfilerConfiguration profilerConfiguration,
      SnapshotProfilingConfiguration snapshotConfiguration) {
    YamlNodeBuilder rootBuilder = YamlNodeBuilder.create();

    addTraceProviderNode(model.getTracerProvider(), rootBuilder);
    addMeterProviderNode(model.getMeterProvider(), rootBuilder);
    addLogsProviderNode(model.getLoggerProvider(), rootBuilder);

    Map<String, Object> root = rootBuilder.build();
    if (root.isEmpty()) {
      return "";
    }
    return toYamlString(root);
  }

  private static void addTraceProviderNode(TracerProviderModel model, YamlNodeBuilder rootBuilder) {
    if (model == null || model.getProcessors() == null) {
      return;
    }

    List<Map<String, Object>> processors = new ArrayList<>();
    model
        .getProcessors()
        .forEach(
            (spanProcessorModel -> {
              if (spanProcessorModel.getBatch() != null) {
                addSpanExporterNode(
                    "batch", spanProcessorModel.getBatch().getExporter(), processors);
              } else if (spanProcessorModel.getSimple() != null) {
                addSpanExporterNode(
                    "simple", spanProcessorModel.getSimple().getExporter(), processors);
              }
            }));

    rootBuilder.addNestedNode("tracer_provider.processors", processors);
  }

  private static void addLogsProviderNode(LoggerProviderModel model, YamlNodeBuilder rootBuilder) {
    if (model == null || model.getProcessors() == null) {
      return;
    }

    List<Map<String, Object>> processors = new ArrayList<>();
    model
        .getProcessors()
        .forEach(
            logProcessorModel -> {
              if (logProcessorModel.getBatch() != null) {
                addLogExporterNode("batch", logProcessorModel.getBatch().getExporter(), processors);
              } else if (logProcessorModel.getSimple() != null) {
                addLogExporterNode(
                    "simple", logProcessorModel.getSimple().getExporter(), processors);
              }
            });

    rootBuilder.addNestedNode("logger_provider.processors", processors);
  }

  private static void addMeterProviderNode(MeterProviderModel model, YamlNodeBuilder rootBuilder) {
    if (model == null || model.getReaders() == null) {
      return;
    }

    List<Map<String, Object>> readers = new ArrayList<>();
    model
        .getReaders()
        .forEach(
            metricReaderModel -> {
              if (metricReaderModel.getPeriodic() != null) {
                addMetricExporterNode(
                    "periodic", metricReaderModel.getPeriodic().getExporter(), readers);
              }
            });

    rootBuilder.addNestedNode("meter_provider.readers", readers);
  }

  private static void addSpanExporterNode(
      String processorType, SpanExporterModel exporter, List<Map<String, Object>> processorList) {
    if (exporter == null) {
      return;
    }
    if (exporter.getOtlpHttp() != null) {
      addOtlpHttpExporterNode(
          processorType, getEndpoint(exporter.getOtlpHttp(), "traces"), processorList);
    } else if (exporter.getOtlpGrpc() != null) {
      addOtlpGrpcExporterNode(processorType, getEndpoint(exporter.getOtlpGrpc()), processorList);
    }
  }

  private static void addLogExporterNode(
      String processorType,
      LogRecordExporterModel exporter,
      List<Map<String, Object>> processorList) {
    if (exporter == null) {
      return;
    }
    if (exporter.getOtlpHttp() != null) {
      addOtlpHttpExporterNode(
          processorType, getEndpoint(exporter.getOtlpHttp(), "logs"), processorList);
    } else if (exporter.getOtlpGrpc() != null) {
      addOtlpGrpcExporterNode(processorType, getEndpoint(exporter.getOtlpGrpc()), processorList);
    }
  }

  private static void addMetricExporterNode(
      String readerType, PushMetricExporterModel exporter, List<Map<String, Object>> readerList) {
    if (exporter == null) {
      return;
    }
    if (exporter.getOtlpHttp() != null) {
      addOtlpHttpExporterNode(readerType, getEndpoint(exporter.getOtlpHttp()), readerList);
    } else if (exporter.getOtlpGrpc() != null) {
      addOtlpGrpcExporterNode(readerType, getEndpoint(exporter.getOtlpGrpc()), readerList);
    }
  }

  private static void addOtlpHttpExporterNode(
      String parentNodeName, String endpoint, List<Map<String, Object>> nodes) {
    nodes.add(
        YamlNodeBuilder.createNestedNode(
            parentNodeName + ".exporter.otlp_http.endpoint", endpoint));
  }

  private static void addOtlpGrpcExporterNode(
      String parentNodeName, String endpoint, List<Map<String, Object>> nodes) {
    nodes.add(
        YamlNodeBuilder.createNestedNode(
            parentNodeName + ".exporter.otlp_grpc.endpoint", endpoint));
  }

  private static String toYamlString(Map<String, Object> rootNode) {
    DumpSettings settings =
        DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(2)
            .setIndentWithIndicator(true)
            .setIndicatorIndent(2)
            .build();

    return new Dump(settings).dumpToString(rootNode);
  }

  private static String getEndpoint(OtlpHttpExporterModel exporter, String signal) {
    return Optional.ofNullable(exporter.getEndpoint()).orElse("http://localhost:4318/v1/" + signal);
  }

  private static String getEndpoint(OtlpGrpcExporterModel exporter) {
    return Optional.ofNullable(exporter.getEndpoint()).orElse(GRPC_DEFAULT_ENDPOINT);
  }

  private static String getEndpoint(OtlpHttpMetricExporterModel exporter) {
    return Optional.ofNullable(exporter.getEndpoint()).orElse("http://localhost:4318/v1/metrics");
  }

  private static String getEndpoint(OtlpGrpcMetricExporterModel exporter) {
    return Optional.ofNullable(exporter.getEndpoint()).orElse(GRPC_DEFAULT_ENDPOINT);
  }

  private static String getProperty(String systemPropertyName) {
    String property = System.getProperty(systemPropertyName);
    if (property == null) {
      String envVarName = toEnvVarName(systemPropertyName);
      property = System.getenv(envVarName);
    }
    return property;
  }

  static String toEnvVarName(String systemPropertyName) {
    return systemPropertyName.toUpperCase().replace('.', '_').replace('-', '_');
  }
}
