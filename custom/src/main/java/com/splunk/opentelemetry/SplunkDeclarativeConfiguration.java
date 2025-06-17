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

package com.splunk.opentelemetry;

import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalProperty;
import static java.util.logging.Level.WARNING;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.setAdditionalProperty;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.addAdditionalPropertyIfAbsent;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalPropertyOrDefault;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SamplerModel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SplunkDeclarativeConfiguration implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger = Logger.getLogger(SplunkDeclarativeConfiguration.class.getName());

  public static final String SPLUNK_ACCESS_TOKEN = "splunk.access.token";
  public static final String PROFILER_ENABLED_PROPERTY = "splunk.profiler.enabled";
  public static final String PROFILER_MEMORY_ENABLED_PROPERTY = "splunk.profiler.memory.enabled";
  public static final String SPLUNK_REALM_PROPERTY = "splunk.realm";
  public static final String SPLUNK_REALM_NONE = "none";

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          ExperimentalLanguageSpecificInstrumentationModel javaModel = Objects.requireNonNull(model.getInstrumentationDevelopment()).getJava();
          if (javaModel == null) {
            return model;
          }
          Map<String, Object> properties = javaModel.getAdditionalProperties();

          customizeConfigProperties(properties);
          customizeSampler(model);
          customizeExportersForSplunkRealm(properties, javaModel);
          customizeExportersForSplunkAccessToken(properties, javaModel);
          customizeLoggers(model, properties);

          // TODO: implement this
//          EndpointProtocolValidator.validate(customized, config, logger::warning);
          return model;
        });
  }

  private static void customizeConfigProperties(Map<String, Object> properties) {
    addAdditionalPropertyIfAbsent(properties, "splunk.metrics.force_full_commandline", false);
    addAdditionalPropertyIfAbsent(properties, "otel.instrumentation.spring-batch.enabled", true);
    addAdditionalPropertyIfAbsent(properties, "otel.instrumentation.spring-batch.item.enabled", true);
  }

  private static void customizeSampler(OpenTelemetryConfigurationModel model) {
    // otel.traces.sampler = always_on
    if (model.getTracerProvider() != null) {
      SamplerModel samplerModel = model.getTracerProvider().getSampler();
      if (samplerModel == null) {
        samplerModel = new SamplerModel();
        model.getTracerProvider().withSampler(samplerModel);
      }
      if (
          (samplerModel.getAlwaysOff() == null) &&
          (samplerModel.getAlwaysOn() == null) &&
          (samplerModel.getJaegerRemote() == null) &&
          (samplerModel.getParentBased() == null) &&
          (samplerModel.getTraceIdRatioBased() == null)
      ) {
        samplerModel.withAlwaysOn(new AlwaysOnSamplerModel());
      }
    }
  }

  private static void customizeLoggers(OpenTelemetryConfigurationModel model, Map<String, Object> properties) {
    // disable logging instrumentations if the logging exporter is not used
    boolean loggerExporterDefined = false;
    if (model.getLoggerProvider() != null) {
      List<LogRecordProcessorModel> processors = model.getLoggerProvider().getProcessors();
      if (processors != null) {
        Optional<LogRecordProcessorModel> found = processors.stream().filter(p -> p.getBatch() != null || p.getSimple() != null).findFirst();
        loggerExporterDefined = found.isPresent();
      }
    }
    if (!loggerExporterDefined) {
      setAdditionalProperty(properties, "otel.instrumentation.java-util-logging.enabled", false);
      setAdditionalProperty(properties, "otel.instrumentation.jboss-logmanager.enabled", false);
      setAdditionalProperty(properties, "otel.instrumentation.log4j-appender.enabled", false);
      setAdditionalProperty(properties, "otel.instrumentation.logback-appender.enabled", false);
    }
  }

  private static void customizeExportersForSplunkAccessToken(Map<String, Object> properties,
      ExperimentalLanguageSpecificInstrumentationModel javaModel) {
    Object accessToken = getAdditionalProperty(properties, SPLUNK_ACCESS_TOKEN);
    if (accessToken != null) {
      // TODO: Must be converted to use Model
      Object userOtlpHeaders = javaModel.getAdditionalProperties().get("otel.exporter.otlp.headers");
      String otlpHeaders =
          (userOtlpHeaders == null ? "" : userOtlpHeaders + ",") + "X-SF-TOKEN=" + accessToken;
      javaModel.setAdditionalProperty("otel.exporter.otlp.headers", otlpHeaders);
    }
  }

  private static void customizeExportersForSplunkRealm(Map<String, Object> properties,
      ExperimentalLanguageSpecificInstrumentationModel javaModel) {
    Object realm = getAdditionalPropertyOrDefault(properties, SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE);
    if (!SPLUNK_REALM_NONE.equals(realm)) {
      // TODO: Must be converted to use Model
      addAdditionalPropertyIfAbsent(properties,
          "otel.exporter.otlp.endpoint",
          "https://ingest." + realm + ".signalfx.com");

      // metrics ingest doesn't currently accept grpc
      // TODO: What to do here if GRPC is already configured? What if there is not metric_provider in config file?
      addAdditionalPropertyIfAbsent(properties, "otel.exporter.otlp.metrics.protocol", "http/protobuf");
      addAdditionalPropertyIfAbsent(
          properties,
          "otel.exporter.otlp.metrics.endpoint",
          "https://ingest." + realm + ".signalfx.com/v2/datapoint/otlp");

      if (getAdditionalProperty(properties, "otel.exporter.otlp.logs.endpoint") == null) {
        String logsEndpoint = getDefaultLogsEndpoint(javaModel);
        logger.log(
            WARNING,
            "Logs can not be sent to {0}, using {1} instead. "
                + "You can override it by setting otel.exporter.otlp.logs.endpoint",
            new Object[] {"https://ingest." + realm + ".signalfx.com", logsEndpoint});

        // TODO: What if there is no logger_provider at all in config file?
        setAdditionalProperty(properties, "otel.exporter.otlp.logs.endpoint", logsEndpoint);
      }
    }
  }

  private static String getDefaultLogsEndpoint(ExperimentalLanguageSpecificInstrumentationModel javaModel) {
    return "http/protobuf".equals(getOtlpLogsProtocol(javaModel))
        ? "http://localhost:4318/v1/logs"
        : "http://localhost:4317";
  }

  public static Object getOtlpLogsProtocol(ExperimentalLanguageSpecificInstrumentationModel javaModel) {
    Object otlpProtocol = javaModel.getAdditionalProperties().get("otel.exporter.otlp.protocol");
    return javaModel.getAdditionalProperties().getOrDefault("otel.exporter.otlp.logs.protocol", otlpProtocol);
  }
}
