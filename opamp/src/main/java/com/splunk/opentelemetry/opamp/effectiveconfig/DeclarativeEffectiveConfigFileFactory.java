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

package com.splunk.opentelemetry.opamp.effectiveconfig;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.opamp.DeclarativeConfigurationInterceptor;
import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LoggerProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MeterProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PushMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;

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
    ProfilerConfiguration profilerConfiguration = ProfilerConfiguration.SUPPLIER.get();
    SnapshotProfilingConfiguration snapshotConfiguration =
        SnapshotProfilingConfiguration.SUPPLIER.get();

    return processModel(model, profilerConfiguration, snapshotConfiguration);
  }

  @VisibleForTesting
  String processModel(
      OpenTelemetryConfigurationModel model,
      ProfilerConfiguration profilerConfiguration,
      SnapshotProfilingConfiguration snapshotConfiguration) {
    YamlNodeBuilder root = YamlNodeBuilder.create();

    addConfigFilesNodes(root);
    addTraceProviderNode(model.getTracerProvider(), root);
    addMeterProviderNode(model.getMeterProvider(), root);
    addLogsProviderNode(model.getLoggerProvider(), root);
    addDistributionConfigNode(profilerConfiguration, snapshotConfiguration, root);

    Map<String, Object> yamlTree = root.build();
    if (yamlTree.isEmpty()) {
      return "";
    }
    return toYamlString(yamlTree);
  }

  private static void addDistributionConfigNode(
      ProfilerConfiguration profilerConfiguration,
      SnapshotProfilingConfiguration snapshotConfiguration,
      YamlNodeBuilder root) {

    root.addNestedNode(
        "distribution.splunk.profiling",
        profiling -> {
          addAlwaysOnProfilerNode(profilerConfiguration, profiling);
          addCallgraphsNode(snapshotConfiguration, profiling);
        });
  }

  private static void addCallgraphsNode(
      SnapshotProfilingConfiguration snapshotConfiguration, YamlNodeBuilder profiling) {
    if (snapshotConfiguration.isEnabled()) {
      profiling.addNestedNode(
          "callgraphs.sampling_interval", snapshotConfiguration.getSamplingInterval().toMillis());
    }
  }

  private static void addAlwaysOnProfilerNode(
      ProfilerConfiguration profilerConfiguration, YamlNodeBuilder profiling) {
    if (profilerConfiguration.isEnabled()) {
      profiling.addNestedNode(
          "always_on",
          alwaysOn -> {
            alwaysOn.addNestedNode(
                "cpu_profiler.sampling_interval",
                profilerConfiguration.getCallStackInterval().toMillis());
            if (profilerConfiguration.getMemoryEnabled()) {
              alwaysOn.addNode("memory_profiler", EmptyYamlNode.INSTANCE);
            }
          });
    }
  }

  private static void addConfigFilesNodes(YamlNodeBuilder root) {
    root.addNode("otel_config_file", getProperty("otel.config.file"));
    root.addNode("otel_experimental_config_file", getProperty("otel.experimental.config.file"));
  }

  private static void addTraceProviderNode(TracerProviderModel model, YamlNodeBuilder root) {
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

    if (!processors.isEmpty()) {
      root.addNestedNode("tracer_provider.processors", processors);
    }
  }

  private static void addLogsProviderNode(LoggerProviderModel model, YamlNodeBuilder root) {
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

    if (!processors.isEmpty()) {
      root.addNestedNode("logger_provider.processors", processors);
    }
  }

  private static void addMeterProviderNode(MeterProviderModel model, YamlNodeBuilder root) {
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

    if (!readers.isEmpty()) {
      root.addNestedNode("meter_provider.readers", readers);
    }
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
    addExporterNode(parentNodeName, "otlp_http", endpoint, nodes);
  }

  private static void addOtlpGrpcExporterNode(
      String parentNodeName, String endpoint, List<Map<String, Object>> nodes) {
    addExporterNode(parentNodeName, "otlp_grpc", endpoint, nodes);
  }

  private static void addExporterNode(
      String parentNodeName,
      String exporterName,
      String endpoint,
      List<Map<String, Object>> nodes) {
    nodes.add(
        YamlNodeBuilder.create()
            .addNestedNode(parentNodeName + ".exporter." + exporterName + ".endpoint", endpoint)
            .build());
  }

  private static String toYamlString(Map<String, Object> rootNode) {
    DumpSettings settings =
        DumpSettings.builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(2)
            .setIndentWithIndicator(true)
            .setIndicatorIndent(2)
            .build();

    return new Dump(settings, new EffectiveConfigRepresenter(settings)).dumpToString(rootNode);
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
    return systemPropertyName.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
  }

  private enum EmptyYamlNode {
    INSTANCE
  }

  private static class EffectiveConfigRepresenter extends StandardRepresenter {
    EffectiveConfigRepresenter(DumpSettings settings) {
      super(settings);
      representers.put(
          EmptyYamlNode.class, value -> representScalar(Tag.NULL, "", ScalarStyle.PLAIN));
    }
  }
}
