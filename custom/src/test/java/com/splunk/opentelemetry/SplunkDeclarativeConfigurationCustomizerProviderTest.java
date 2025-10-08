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

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.parseAndCustomizeModel;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.SdkConfigProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SplunkDeclarativeConfigurationCustomizerProviderTest {

  @Test
  void shouldCustomizeConfigPropertiesIfUndefined() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
            """;

    // when
    DeclarativeConfigProperties configProperties = getCustomizedJavaInstrumentationConfig(yaml);

    // then
    assertThat(
            configProperties
                .getStructured("splunk")
                .getStructured("metrics")
                .getBoolean("force_full_commandline"))
        .isEqualTo(false);
    assertThat(configProperties.getStructured("spring-batch").getBoolean("enabled"))
        .isEqualTo(true);
    assertThat(
            configProperties
                .getStructured("spring-batch")
                .getStructured("item")
                .getBoolean("enabled"))
        .isEqualTo(true);
  }

  @Test
  void shouldKeepOriginalConfigProperties() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
                spring-batch:
                  enabled: false
                  item:
                    enabled: false
                splunk:
                  metrics:
                    force_full_commandline: true
            """;

    // when
    DeclarativeConfigProperties configProperties = getCustomizedJavaInstrumentationConfig(yaml);

    // then
    assertThat(
            configProperties
                .getStructured("splunk")
                .getStructured("metrics")
                .getBoolean("force_full_commandline"))
        .isEqualTo(true);
    assertThat(configProperties.getStructured("spring-batch").getBoolean("enabled"))
        .isEqualTo(false);
    assertThat(
            configProperties
                .getStructured("spring-batch")
                .getStructured("item")
                .getBoolean("enabled"))
        .isEqualTo(false);
  }

  @Test
  void shouldCustomizeSamplerIfUndefined(@TempDir Path tempDir) {
    // given
    String yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      console:
              sampler:
            instrumentation/development:
              java:
            """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model.getTracerProvider().getSampler().getAlwaysOn()).isNotNull();
  }

  @Test
  void shouldKeepOriginalSamplerConfigurationIfDefined(@TempDir Path tempDir) {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      console:
              sampler:
                always_off:
            instrumentation/development:
              java:
            """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model.getTracerProvider().getSampler().getAlwaysOff()).isNotNull();
  }

  @Test
  void shouldCustomizeSpanExporters() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
                - batch:
                    exporter:
                      otlp_http:
                        endpoint: "http://untouched:1"
                - batch:
                    exporter:
                      otlp_grpc:
                - batch:
                    exporter:
                      otlp_grpc:
                        endpoint: "http://untouched:2"
                - simple:
                    exporter:
                      otlp_http:
                - simple:
                    exporter:
                      otlp_http:
                        endpoint: "http://untouched:3"
                - simple:
                    exporter:
                      otlp_grpc:
                - simple:
                    exporter:
                      otlp_grpc:
                        endpoint: "http://untouched:4"
            instrumentation/development:
              java:
                splunk:
                  realm: unreal-test-realm
                  access:
                    token: ABC123456
            """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    NameStringValuePairModel tokenHeader =
        new NameStringValuePairModel().withName("X-SF-TOKEN").withValue("ABC123456");
    String httpEndpoint = "https://ingest.unreal-test-realm.signalfx.com/v1/traces";
    String grpcEndpoint = "https://ingest.unreal-test-realm.signalfx.com";

    assertThat(getSpanProcessorModel(model, 0).getBatch().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals(httpEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getSpanProcessorModel(model, 1).getBatch().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals("http://untouched:1"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getSpanProcessorModel(model, 2).getBatch().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals(grpcEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getSpanProcessorModel(model, 3).getBatch().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals("http://untouched:2"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getSpanProcessorModel(model, 4).getSimple().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals(httpEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getSpanProcessorModel(model, 5).getSimple().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals("http://untouched:3"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getSpanProcessorModel(model, 6).getSimple().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals(grpcEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getSpanProcessorModel(model, 7).getSimple().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals("http://untouched:4"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));
  }

  @Test
  void shouldCustomizeMetricExporters() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            meter_provider:
              readers:
                - periodic:
                    exporter:
                      otlp_http:
                - periodic:
                    exporter:
                      otlp_http:
                        endpoint: "http://untouched:1"
                - periodic:
                    exporter:
                      otlp_grpc:
                - periodic:
                    exporter:
                      otlp_grpc:
                        endpoint: "http://untouched:2"
            instrumentation/development:
              java:
                splunk:
                  realm: unreal-test-realm
                  access:
                    token: ABC123456
            """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    NameStringValuePairModel tokenHeader =
        new NameStringValuePairModel().withName("X-SF-TOKEN").withValue("ABC123456");
    String httpEndpoint = "https://ingest.unreal-test-realm.signalfx.com/v2/datapoint/otlp";

    assertThat(getMetricReaderModel(model, 0).getPeriodic().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals(httpEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getMetricReaderModel(model, 1).getPeriodic().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals("http://untouched:1"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getMetricReaderModel(model, 2).getPeriodic().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint() == null)
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getMetricReaderModel(model, 3).getPeriodic().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals("http://untouched:2"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));
  }

  @Test
  void shouldCustomizeLogRecordExporters() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            logger_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
                - batch:
                    exporter:
                      otlp_http:
                        endpoint: "http://untouched:1"
                - batch:
                    exporter:
                      otlp_grpc:
                - batch:
                    exporter:
                      otlp_grpc:
                        endpoint: "http://untouched:2"
                - simple:
                    exporter:
                      otlp_http:
                - simple:
                    exporter:
                      otlp_http:
                        endpoint: "http://untouched:3"
                - simple:
                    exporter:
                      otlp_grpc:
                - simple:
                    exporter:
                      otlp_grpc:
                        endpoint: "http://untouched:4"
            instrumentation/development:
              java:
                splunk:
                  realm: unreal-test-realm
                  access:
                    token: ABC123456
            """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    NameStringValuePairModel tokenHeader =
        new NameStringValuePairModel().withName("X-SF-TOKEN").withValue("ABC123456");
    String httpEndpoint = "http://localhost:4318/v1/logs";
    String grpcEndpoint = "http://localhost:4317";

    assertThat(getLogRecordProcessorModel(model, 0).getBatch().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals(httpEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getLogRecordProcessorModel(model, 1).getBatch().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals("http://untouched:1"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getLogRecordProcessorModel(model, 2).getBatch().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals(grpcEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getLogRecordProcessorModel(model, 3).getBatch().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals("http://untouched:2"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getLogRecordProcessorModel(model, 4).getSimple().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals(httpEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getLogRecordProcessorModel(model, 5).getSimple().getExporter().getOtlpHttp())
        .matches((m) -> m.getEndpoint().equals("http://untouched:3"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getLogRecordProcessorModel(model, 6).getSimple().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals(grpcEndpoint))
        .matches((m) -> m.getHeaders().contains(tokenHeader));

    assertThat(getLogRecordProcessorModel(model, 7).getSimple().getExporter().getOtlpGrpc())
        .matches((m) -> m.getEndpoint().equals("http://untouched:4"))
        .matches((m) -> m.getHeaders().contains(tokenHeader));
  }

  private SpanProcessorModel getSpanProcessorModel(
      OpenTelemetryConfigurationModel model, int index) {
    assertThat(model.getTracerProvider()).isNotNull();
    return model.getTracerProvider().getProcessors().get(index);
  }

  private MetricReaderModel getMetricReaderModel(OpenTelemetryConfigurationModel model, int index) {
    assertThat(model.getMeterProvider()).isNotNull();
    return model.getMeterProvider().getReaders().get(index);
  }

  private LogRecordProcessorModel getLogRecordProcessorModel(
      OpenTelemetryConfigurationModel model, int index) {
    assertThat(model.getLoggerProvider()).isNotNull();
    return model.getLoggerProvider().getProcessors().get(index);
  }

  @Test
  void shouldNotUpdateAccessTokenProvidedInExporterHeaders() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
                        headers:
                          - name: X-SF-TOKEN
                            value: XYZ
            instrumentation/development:
              java:
                splunk:
                  access:
                    token: ABC123456
            """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    List<NameStringValuePairModel> headers =
        model.getTracerProvider().getProcessors().stream()
            .filter(m -> m.getBatch() != null)
            .limit(1)
            .toList()
            .get(0)
            .getBatch()
            .getExporter()
            .getOtlpHttp()
            .getHeaders();
    assertThat(headers.size()).isEqualTo(1);
    assertThat(headers.get(0).getName()).isEqualTo("X-SF-TOKEN");
    assertThat(headers.get(0).getValue()).isEqualTo("XYZ");
  }

  private static OpenTelemetryConfigurationModel getCustomizedModel(String yaml) {
    SplunkDeclarativeConfigurationCustomizerProvider customizer =
        new SplunkDeclarativeConfigurationCustomizerProvider();
    return parseAndCustomizeModel(yaml, customizer);
  }

  private static DeclarativeConfigProperties getCustomizedJavaInstrumentationConfig(String yaml) {
    var model = getCustomizedModel(yaml);
    var configProvider = SdkConfigProvider.create(model);
    return Objects.requireNonNull(configProvider.getInstrumentationConfig()).getStructured("java");
  }
}
