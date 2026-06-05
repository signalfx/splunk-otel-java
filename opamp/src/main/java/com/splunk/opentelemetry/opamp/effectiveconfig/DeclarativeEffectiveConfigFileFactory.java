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
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.EffectiveConfigYamlMapper;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.EmptyYamlNode;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model.DistributionYamlModel;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model.EffectiveConfigYamlModel;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model.ExporterYamlModel;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model.LoggerProviderYamlModel;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model.MeterProviderYamlModel;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model.ProfilingYamlModel;
import com.splunk.opentelemetry.opamp.effectiveconfig.yaml.model.TracerProviderYamlModel;
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
import java.util.Map;
import java.util.Optional;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;

public class DeclarativeEffectiveConfigFileFactory implements EffectiveConfigFactory {
  private static final String GRPC_DEFAULT_ENDPOINT = "http://localhost:4317";

  public DeclarativeEffectiveConfigFileFactory() {}

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
    EffectiveConfigYamlModel root = new EffectiveConfigYamlModel();

    addDeclarativeConfigFileNodes(root);
    addTraceProviderNode(model.getTracerProvider(), root.tracerProvider());
    addMeterProviderNode(model.getMeterProvider(), root.meterProvider());
    addLogsProviderNode(model.getLoggerProvider(), root.loggerProvider());
    addDistributionConfigNode(profilerConfiguration, snapshotConfiguration, root.distribution());

    Map<String, Object> yamlTree = EffectiveConfigYamlMapper.toYamlMap(root);
    if (yamlTree.isEmpty()) {
      return "";
    }
    return toYamlString(yamlTree);
  }

  private static void addDeclarativeConfigFileNodes(EffectiveConfigYamlModel root) {
    root.configFiles(getProperty("otel.config.file"), getProperty("otel.experimental.config.file"));
  }

  private static void addDistributionConfigNode(
      ProfilerConfiguration profilerConfiguration,
      SnapshotProfilingConfiguration snapshotConfiguration,
      DistributionYamlModel distribution) {

    ProfilingYamlModel profiling = distribution.profiling();
    addAlwaysOnProfilerNode(profilerConfiguration, profiling);
    addCallgraphsNode(snapshotConfiguration, profiling);
  }

  private static void addCallgraphsNode(
      SnapshotProfilingConfiguration snapshotConfiguration, ProfilingYamlModel profiling) {
    if (snapshotConfiguration.isEnabled()) {
      profiling.callgraphs(snapshotConfiguration.getSamplingInterval().toMillis());
    }
  }

  private static void addAlwaysOnProfilerNode(
      ProfilerConfiguration profilerConfiguration, ProfilingYamlModel profiling) {
    if (profilerConfiguration.isEnabled()) {
      profiling.alwaysOn(
          alwaysOn -> {
            alwaysOn.cpuProfiler(profilerConfiguration.getCallStackInterval().toMillis());
            if (profilerConfiguration.getMemoryEnabled()) {
              alwaysOn.memoryProfiler();
            }
          });
    }
  }

  private static void addTraceProviderNode(
      TracerProviderModel model, TracerProviderYamlModel tracerProvider) {
    if (model == null || model.getProcessors() == null) {
      return;
    }

    model
        .getProcessors()
        .forEach(
            (spanProcessorModel -> {
              if (spanProcessorModel.getBatch() != null) {
                addSpanExporterNode(
                    "batch", spanProcessorModel.getBatch().getExporter(), tracerProvider);
              } else if (spanProcessorModel.getSimple() != null) {
                addSpanExporterNode(
                    "simple", spanProcessorModel.getSimple().getExporter(), tracerProvider);
              }
            }));
  }

  private static void addLogsProviderNode(
      LoggerProviderModel model, LoggerProviderYamlModel loggerProvider) {
    if (model == null || model.getProcessors() == null) {
      return;
    }

    model
        .getProcessors()
        .forEach(
            logProcessorModel -> {
              if (logProcessorModel.getBatch() != null) {
                addLogExporterNode(
                    "batch", logProcessorModel.getBatch().getExporter(), loggerProvider);
              } else if (logProcessorModel.getSimple() != null) {
                addLogExporterNode(
                    "simple", logProcessorModel.getSimple().getExporter(), loggerProvider);
              }
            });
  }

  private static void addMeterProviderNode(
      MeterProviderModel model, MeterProviderYamlModel meterProvider) {
    if (model == null || model.getReaders() == null) {
      return;
    }

    model
        .getReaders()
        .forEach(
            metricReaderModel -> {
              if (metricReaderModel.getPeriodic() != null) {
                addMetricExporterNode(
                    "periodic", metricReaderModel.getPeriodic().getExporter(), meterProvider);
              }
            });
  }

  private static void addSpanExporterNode(
      String processorType, SpanExporterModel exporter, TracerProviderYamlModel tracerProvider) {
    ExporterYamlModel effectiveExporter = createSpanExporter(exporter);
    if (effectiveExporter != null) {
      tracerProvider.addProcessor(processorType, effectiveExporter);
    }
  }

  private static void addLogExporterNode(
      String processorType,
      LogRecordExporterModel exporter,
      LoggerProviderYamlModel loggerProvider) {
    ExporterYamlModel effectiveExporter = createLogExporter(exporter);
    if (effectiveExporter != null) {
      loggerProvider.addProcessor(processorType, effectiveExporter);
    }
  }

  private static void addMetricExporterNode(
      String readerType, PushMetricExporterModel exporter, MeterProviderYamlModel meterProvider) {
    ExporterYamlModel effectiveExporter = createMetricExporter(exporter);
    if (effectiveExporter != null) {
      meterProvider.addReader(readerType, effectiveExporter);
    }
  }

  private static ExporterYamlModel createSpanExporter(SpanExporterModel exporter) {
    if (exporter == null) {
      return null;
    }
    if (exporter.getOtlpHttp() != null) {
      return ExporterYamlModel.otlpHttp(getEndpoint(exporter.getOtlpHttp(), "traces"));
    } else if (exporter.getOtlpGrpc() != null) {
      return ExporterYamlModel.otlpGrpc(getEndpoint(exporter.getOtlpGrpc()));
    }
    return null;
  }

  private static ExporterYamlModel createLogExporter(LogRecordExporterModel exporter) {
    if (exporter == null) {
      return null;
    }
    if (exporter.getOtlpHttp() != null) {
      return ExporterYamlModel.otlpHttp(getEndpoint(exporter.getOtlpHttp(), "logs"));
    } else if (exporter.getOtlpGrpc() != null) {
      return ExporterYamlModel.otlpGrpc(getEndpoint(exporter.getOtlpGrpc()));
    }
    return null;
  }

  private static ExporterYamlModel createMetricExporter(PushMetricExporterModel exporter) {
    if (exporter == null) {
      return null;
    }
    if (exporter.getOtlpHttp() != null) {
      return ExporterYamlModel.otlpHttp(getEndpoint(exporter.getOtlpHttp()));
    } else if (exporter.getOtlpGrpc() != null) {
      return ExporterYamlModel.otlpGrpc(getEndpoint(exporter.getOtlpGrpc()));
    }
    return null;
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
    return systemPropertyName.toUpperCase().replace('.', '_').replace('-', '_');
  }

  private static class EffectiveConfigRepresenter extends StandardRepresenter {
    EffectiveConfigRepresenter(DumpSettings settings) {
      super(settings);
      representers.put(
          EmptyYamlNode.class, value -> representScalar(Tag.NULL, "", ScalarStyle.PLAIN));
    }
  }
}
