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

import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.addAdditionalPropertyIfAbsent;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalProperty;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.getAdditionalPropertyOrDefault;
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.setAdditionalProperty;
import static java.util.logging.Level.WARNING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SamplerModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SplunkDeclarativeConfiguration implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger =
      Logger.getLogger(SplunkDeclarativeConfiguration.class.getName());

  public static final String SPLUNK_ACCESS_TOKEN = "splunk.access.token";
  public static final String PROFILER_ENABLED_PROPERTY = "splunk.profiler.enabled";
  public static final String PROFILER_MEMORY_ENABLED_PROPERTY = "splunk.profiler.memory.enabled";
  public static final String SPLUNK_REALM_PROPERTY = "splunk.realm";
  public static final String SPLUNK_REALM_NONE = "none";

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          if (model.getInstrumentationDevelopment() == null) {
            return model;
          }
          ExperimentalLanguageSpecificInstrumentationModel javaModel = model.getInstrumentationDevelopment().getJava();
          if (javaModel == null) {
            return model;
          }
          Map<String, Object> properties = javaModel.getAdditionalProperties();

          customizeConfigProperties(properties);
          customizeSampler(model);
          customizeExportersForSplunkRealm(model, properties, javaModel);
          customizeExportersForSplunkAccessToken(model, properties);
          customizeLoggers(model, properties);

          // TODO: implement this
          //          EndpointProtocolValidator.validate(customized, config, logger::warning);
          //          logger.fine(() -> "Splunk configuration customization complete: " + model);

          // TODO: This is temporary code. Remove it before release !!!
          ObjectMapper mapper = new ObjectMapper();
          String v = null;
          try {
            v =
                mapper
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .writeValueAsString(model);
            logger.info("Splunk configuration customization complete: " + v);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
          return model;
        });
  }

  private static void customizeConfigProperties(Map<String, Object> properties) {
    addAdditionalPropertyIfAbsent(properties, "splunk.metrics.force_full_commandline", false);
    addAdditionalPropertyIfAbsent(properties, "otel.instrumentation.spring-batch.enabled", true);
    addAdditionalPropertyIfAbsent(
        properties, "otel.instrumentation.spring-batch.item.enabled", true);
  }

  private static void customizeSampler(OpenTelemetryConfigurationModel model) {
    // otel.traces.sampler = always_on
    if (model.getTracerProvider() != null) {
      SamplerModel samplerModel = model.getTracerProvider().getSampler();
      if (samplerModel == null) {
        samplerModel = new SamplerModel();
        model.getTracerProvider().withSampler(samplerModel);
      }
      if ((samplerModel.getAlwaysOff() == null)
          && (samplerModel.getAlwaysOn() == null)
          && (samplerModel.getJaegerRemote() == null)
          && (samplerModel.getParentBased() == null)
          && (samplerModel.getTraceIdRatioBased() == null)) {
        samplerModel.withAlwaysOn(new AlwaysOnSamplerModel());
      }
    }
  }

  private static void customizeLoggers(
      OpenTelemetryConfigurationModel model, Map<String, Object> properties) {
    // disable logging instrumentations if the logging exporter is not used
    boolean loggerExporterDefined = false;
    if (model.getLoggerProvider() != null) {
      List<LogRecordProcessorModel> processors = model.getLoggerProvider().getProcessors();
      if (processors != null) {
        Optional<LogRecordProcessorModel> found =
            processors.stream()
                .filter(p -> p.getBatch() != null || p.getSimple() != null)
                .findFirst();
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

  // TODO: Can't we just enforce token put directly in the YAML in exporters' headers ?
  private static void customizeExportersForSplunkAccessToken(
      OpenTelemetryConfigurationModel model, Map<String, Object> properties) {
    Object accessToken = getAdditionalProperty(properties, SPLUNK_ACCESS_TOKEN);
    if (accessToken != null) {
      if (model.getTracerProvider() != null) {
        iterateBatchSpanExporters(model.getTracerProvider())
            .map(SpanExporterModel::getOtlpHttp)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
        iterateBatchSpanExporters(model.getTracerProvider())
            .map(SpanExporterModel::getOtlpGrpc)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));

        iterateSimpleSpanExporters(model.getTracerProvider())
            .map(SpanExporterModel::getOtlpHttp)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
        iterateSimpleSpanExporters(model.getTracerProvider())
            .map(SpanExporterModel::getOtlpGrpc)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
      }

      if (model.getMeterProvider() != null) {
        iteratePeriodicMetricExporters(model.getMeterProvider())
            .map(PushMetricExporterModel::getOtlpHttp)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
        iteratePeriodicMetricExporters(model.getMeterProvider())
            .map(PushMetricExporterModel::getOtlpGrpc)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
      }

      if (model.getLoggerProvider() != null) {
        iterateBatchLogRecordExporters(model.getLoggerProvider())
            .map(LogRecordExporterModel::getOtlpHttp)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
        iterateBatchLogRecordExporters(model.getLoggerProvider())
            .map(LogRecordExporterModel::getOtlpGrpc)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));

        iterateSimpleLogRecordExporters(model.getLoggerProvider())
            .map(LogRecordExporterModel::getOtlpHttp)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
        iterateSimpleLogRecordExporters(model.getLoggerProvider())
            .map(LogRecordExporterModel::getOtlpGrpc)
            .filter(Objects::nonNull)
            .forEach(
                exporterTransportModel ->
                    addTokenHeader(exporterTransportModel, accessToken.toString()));
      }
    }
  }

  private static Stream<SpanExporterModel> iterateBatchSpanExporters(TracerProviderModel model) {
    return model.getProcessors().stream()
        .map(SpanProcessorModel::getBatch)
        .filter(Objects::nonNull)
        .map(BatchSpanProcessorModel::getExporter)
        .filter(Objects::nonNull);
  }

  private static Stream<SpanExporterModel> iterateSimpleSpanExporters(TracerProviderModel model) {
    return model.getProcessors().stream()
        .map(SpanProcessorModel::getSimple)
        .filter(Objects::nonNull)
        .map(SimpleSpanProcessorModel::getExporter)
        .filter(Objects::nonNull);
  }

  private static Stream<PushMetricExporterModel> iteratePeriodicMetricExporters(
      MeterProviderModel model) {
    return model.getReaders().stream()
        .map(MetricReaderModel::getPeriodic)
        .filter(Objects::nonNull)
        .map(PeriodicMetricReaderModel::getExporter)
        .filter(Objects::nonNull);
  }

  private static Stream<LogRecordExporterModel> iterateBatchLogRecordExporters(
      LoggerProviderModel model) {
    return model.getProcessors().stream()
        .map(LogRecordProcessorModel::getBatch)
        .filter(Objects::nonNull)
        .map(BatchLogRecordProcessorModel::getExporter)
        .filter(Objects::nonNull);
  }

  private static Stream<LogRecordExporterModel> iterateSimpleLogRecordExporters(
      LoggerProviderModel model) {
    return model.getProcessors().stream()
        .map(LogRecordProcessorModel::getSimple)
        .filter(Objects::nonNull)
        .map(SimpleLogRecordProcessorModel::getExporter)
        .filter(Objects::nonNull);
  }

  private static void addTokenHeader(OtlpHttpExporterModel model, String accessToken) {
    List<NameStringValuePairModel> headers = model.getHeaders();
    if (headers == null) {
      headers = new ArrayList<>();
      model.withHeaders(headers);
    }
    addTokenHeader(headers, accessToken);
  }

  private static void addTokenHeader(OtlpGrpcMetricExporterModel model, String accessToken) {
    List<NameStringValuePairModel> headers = model.getHeaders();
    if (headers == null) {
      headers = new ArrayList<>();
      model.withHeaders(headers);
    }
    addTokenHeader(headers, accessToken);
  }

  private static void addTokenHeader(OtlpHttpMetricExporterModel model, String accessToken) {
    List<NameStringValuePairModel> headers = model.getHeaders();
    if (headers == null) {
      headers = new ArrayList<>();
      model.withHeaders(headers);
    }
    addTokenHeader(headers, accessToken);
  }

  private static void addTokenHeader(OtlpGrpcExporterModel model, String accessToken) {
    List<NameStringValuePairModel> headers = model.getHeaders();
    if (headers == null) {
      headers = new ArrayList<>();
      model.withHeaders(headers);
    }
    addTokenHeader(headers, accessToken);
  }

  private static void addTokenHeader(List<NameStringValuePairModel> headers, String accessToken) {
    NameStringValuePairModel accessTokenHeader =
        new NameStringValuePairModel().withName("X-SF-TOKEN").withValue(accessToken);
    if (headers.stream()
        .noneMatch(header -> Objects.equals(header.getName(), accessTokenHeader.getName()))) {
      headers.add(accessTokenHeader);
    }
  }

  private static void customizeExportersForSplunkRealm(
      OpenTelemetryConfigurationModel model,
      Map<String, Object> properties,
      ExperimentalLanguageSpecificInstrumentationModel javaModel) {
    Object realm =
        getAdditionalPropertyOrDefault(properties, SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE);
    if (!SPLUNK_REALM_NONE.equals(realm)) {
      String realmBaseUrl = "https://ingest." + realm + ".signalfx.com";

      // Trace providers exporter configuration
      TracerProviderModel tracerProviderModel = model.getTracerProvider();
      if (tracerProviderModel == null) {
        tracerProviderModel = new TracerProviderModel();
        model.withTracerProvider(tracerProviderModel);
      }
      List<SpanProcessorModel> spanProcessors = tracerProviderModel.getProcessors();
      if (spanProcessors.isEmpty()) {
        spanProcessors = new ArrayList<>();
        tracerProviderModel.withProcessors(spanProcessors);

        OtlpHttpExporterModel httpExporterModel =
            new OtlpHttpExporterModel()
                .withEndpoint(realmBaseUrl + "/v1/traces")
                .withEncoding(OtlpHttpExporterModel.OtlpHttpEncoding.PROTOBUF);
        spanProcessors.add(
            new SpanProcessorModel()
                .withBatch(
                    new BatchSpanProcessorModel()
                        .withExporter(new SpanExporterModel().withOtlpHttp(httpExporterModel))));
      }

      // Meter providers exporter configuration
      MeterProviderModel meterProviderModel = model.getMeterProvider();
      if (meterProviderModel == null) {
        meterProviderModel = new MeterProviderModel();
        model.withMeterProvider(meterProviderModel);
      }
      List<MetricReaderModel> readers = meterProviderModel.getReaders();
      if (readers.isEmpty()) {
        readers = new ArrayList<>();
        meterProviderModel.withReaders(readers);

        OtlpHttpMetricExporterModel httpExporterModel =
            new OtlpHttpMetricExporterModel()
                .withEndpoint(realmBaseUrl + "/v2/datapoint/otlp")
                .withEncoding(OtlpHttpExporterModel.OtlpHttpEncoding.PROTOBUF);
        readers.add(
            new MetricReaderModel()
                .withPeriodic(
                    new PeriodicMetricReaderModel()
                        .withExporter(
                            new PushMetricExporterModel().withOtlpHttp(httpExporterModel))));
      }

      // Log providers exporter configuration
      // Just accept what is defined in the YAML, or add http/protobuf exporter.
      // Flat config provides a support for GRPC based logger, but with file based config it is
      // impossible
      LoggerProviderModel loggerProviderModel = model.getLoggerProvider();
      if (loggerProviderModel == null) {
        loggerProviderModel = new LoggerProviderModel();
        model.withLoggerProvider(loggerProviderModel);
      }
      List<LogRecordProcessorModel> processors = loggerProviderModel.getProcessors();
      if (processors.isEmpty()) {
        processors = new ArrayList<>();
        loggerProviderModel.withProcessors(processors);

        String logsEndpoint = "http://localhost:4318/v1/logs";
        OtlpHttpExporterModel httpExporterModel =
            new OtlpHttpExporterModel()
                .withEndpoint(logsEndpoint)
                .withEncoding(OtlpHttpExporterModel.OtlpHttpEncoding.PROTOBUF);
        processors.add(
            new LogRecordProcessorModel()
                .withBatch(
                    new BatchLogRecordProcessorModel()
                        .withExporter(
                            new LogRecordExporterModel().withOtlpHttp(httpExporterModel))));

        logger.log(
            WARNING,
            "Logs can not be sent to {0}, using {1} instead. "
                + "You can override it by providing log exporter definition in YAML config file.",
            new Object[] {realmBaseUrl, logsEndpoint});
      }
    }
  }
}
