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
import static io.opentelemetry.sdk.autoconfigure.AdditionalPropertiesUtil.setAdditionalProperty;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.InstrumentationModel;
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
public class SplunkDeclarativeConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private static final Logger logger =
      Logger.getLogger(SplunkDeclarativeConfigurationCustomizerProvider.class.getName());

  public static final String SPLUNK_ACCESS_TOKEN = "splunk.access.token";
  public static final String SPLUNK_REALM_PROPERTY = "splunk.realm";

  @Override
  public void customize(DeclarativeConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addModelCustomizer(
        model -> {
          if (model.getInstrumentationDevelopment() == null) {
            model.withInstrumentationDevelopment(new InstrumentationModel());
          }
          ExperimentalLanguageSpecificInstrumentationModel javaModel =
              model.getInstrumentationDevelopment().getJava();
          if (javaModel == null) {
            javaModel = new ExperimentalLanguageSpecificInstrumentationModel();
            model.getInstrumentationDevelopment().withJava(javaModel);
          }
          Map<String, Object> properties = javaModel.getAdditionalProperties();

          customizeConfigProperties(properties);
          customizeExporters(model, properties);
          customizeSampler(model);
          customizeLoggers(model, properties);

          logger.fine(() -> "Customized model: " + model);

          // TODO: implement this in the declarative config validator
          //          EndpointProtocolValidator.validate(customized, config, logger::warning);
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
          && (samplerModel.getTraceIdRatioBased() == null)
          && (samplerModel.getAdditionalProperties().isEmpty())) {
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

  private static void customizeExporters(
      OpenTelemetryConfigurationModel model, Map<String, Object> properties) {
    String accessToken =
        Optional.ofNullable(getAdditionalProperty(properties, SPLUNK_ACCESS_TOKEN))
            .map(Object::toString)
            .orElse(null);
    String baseEndpointUrl =
        Optional.ofNullable(getAdditionalProperty(properties, SPLUNK_REALM_PROPERTY))
            .map((realm) -> "https://ingest." + realm + ".signalfx.com")
            .orElse(null);

    if (model.getTracerProvider() != null) {
      customizeSpanExporters(model.getTracerProvider(), baseEndpointUrl, accessToken);
    }

    if (model.getMeterProvider() != null) {
      customizeMetricExporters(model.getMeterProvider(), baseEndpointUrl, accessToken);
    }

    if (model.getLoggerProvider() != null) {
      customizeLogRecordExporters(model.getLoggerProvider(), accessToken);
    }
  }

  private static void customizeSpanExporters(
      TracerProviderModel model, String baseEndpointUrl, String accessToken) {
    String endpointUrl =
        Optional.ofNullable(baseEndpointUrl).map((url) -> url + "/v1/traces").orElse(null);

    iterateBatchSpanExporters(model)
        .map(SpanExporterModel::getOtlpHttp)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, endpointUrl);
            });
    iterateBatchSpanExporters(model)
        .map(SpanExporterModel::getOtlpGrpc)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, baseEndpointUrl);
            });

    iterateSimpleSpanExporters(model)
        .map(SpanExporterModel::getOtlpHttp)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, endpointUrl);
            });
    iterateSimpleSpanExporters(model)
        .map(SpanExporterModel::getOtlpGrpc)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, baseEndpointUrl);
            });
  }

  private static void customizeMetricExporters(
      MeterProviderModel model, String baseEndpointUrl, String accessToken) {
    String endpointUrl =
        Optional.ofNullable(baseEndpointUrl).map((url) -> url + "/v2/datapoint/otlp").orElse(null);

    iteratePeriodicMetricExporters(model)
        .map(PushMetricExporterModel::getOtlpHttp)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, endpointUrl);
            });
    iteratePeriodicMetricExporters(model)
        .map(PushMetricExporterModel::getOtlpGrpc)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              // metrics ingest doesn't currently accept grpc
            });
  }

  private static void customizeLogRecordExporters(LoggerProviderModel model, String accessToken) {
    String httpEndpoint = "http://localhost:4318/v1/logs";
    String grpcEndpoint = "http://localhost:4317";

    iterateBatchLogRecordExporters(model)
        .map(LogRecordExporterModel::getOtlpHttp)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, httpEndpoint);
            });
    iterateBatchLogRecordExporters(model)
        .map(LogRecordExporterModel::getOtlpGrpc)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, grpcEndpoint);
            });

    iterateSimpleLogRecordExporters(model)
        .map(LogRecordExporterModel::getOtlpHttp)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, httpEndpoint);
            });
    iterateSimpleLogRecordExporters(model)
        .map(LogRecordExporterModel::getOtlpGrpc)
        .filter(Objects::nonNull)
        .forEach(
            exporterModel -> {
              maybeAddTokenHeader(exporterModel, accessToken);
              maybeSetEndpoint(exporterModel, grpcEndpoint);
            });
  }

  private static void maybeSetEndpoint(OtlpHttpMetricExporterModel exporterModel, String endpoint) {
    if ((endpoint != null) && (exporterModel.getEndpoint() == null)) {
      exporterModel.withEndpoint(endpoint);
      exporterModel.withEncoding(OtlpHttpExporterModel.OtlpHttpEncoding.PROTOBUF);
    }
  }

  private static void maybeSetEndpoint(OtlpGrpcExporterModel exporterModel, String endpoint) {
    if ((endpoint != null) && (exporterModel.getEndpoint() == null)) {
      exporterModel.withEndpoint(endpoint);
    }
  }

  private static void maybeSetEndpoint(OtlpHttpExporterModel exporterModel, String endpoint) {
    if ((endpoint != null) && (exporterModel.getEndpoint() == null)) {
      exporterModel.withEndpoint(endpoint);
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

  private static void maybeAddTokenHeader(OtlpHttpExporterModel model, String accessToken) {
    if (accessToken != null) {
      List<NameStringValuePairModel> headers = model.getHeaders();
      if (headers == null) {
        headers = new ArrayList<>();
        model.withHeaders(headers);
      }
      maybeAddTokenHeader(headers, accessToken);
    }
  }

  private static void maybeAddTokenHeader(OtlpGrpcMetricExporterModel model, String accessToken) {
    if (accessToken != null) {
      List<NameStringValuePairModel> headers = model.getHeaders();
      if (headers == null) {
        headers = new ArrayList<>();
        model.withHeaders(headers);
      }
      maybeAddTokenHeader(headers, accessToken);
    }
  }

  private static void maybeAddTokenHeader(OtlpHttpMetricExporterModel model, String accessToken) {
    if (accessToken != null) {
      List<NameStringValuePairModel> headers = model.getHeaders();
      if (headers == null) {
        headers = new ArrayList<>();
        model.withHeaders(headers);
      }
      maybeAddTokenHeader(headers, accessToken);
    }
  }

  private static void maybeAddTokenHeader(OtlpGrpcExporterModel model, String accessToken) {
    if (accessToken != null) {
      List<NameStringValuePairModel> headers = model.getHeaders();
      if (headers == null) {
        headers = new ArrayList<>();
        model.withHeaders(headers);
      }
      maybeAddTokenHeader(headers, accessToken);
    }
  }

  private static void maybeAddTokenHeader(
      List<NameStringValuePairModel> headers, String accessToken) {
    NameStringValuePairModel accessTokenHeader =
        new NameStringValuePairModel().withName("X-SF-TOKEN").withValue(accessToken);
    if (headers.stream()
        .noneMatch(header -> Objects.equals(header.getName(), accessTokenHeader.getName()))) {
      headers.add(accessTokenHeader);
    }
  }
}
