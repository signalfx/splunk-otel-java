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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.SdkConfigProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpMetricExporterModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class SplunkDeclarativeConfigurationCustomizerProviderTest {
  @RegisterExtension
  private static final DeclarativeConfigTestExtension configTestExtension =
      DeclarativeConfigTestExtension.create();

  @Test
  void shouldCustomizeConfigPropertiesIfUndefined() {
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
            """;

    DeclarativeConfigProperties configProperties = getCustomizedJavaInstrumentationConfig(yaml);

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

    DeclarativeConfigProperties configProperties = getCustomizedJavaInstrumentationConfig(yaml);

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
  void shouldCustomizeSamplerIfUndefined(@TempDir Path tempDir) throws IOException {
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

    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    assertThat(model.getTracerProvider().getSampler().getAlwaysOn()).isNotNull();
  }

  @Test
  void shouldKeepOriginalSamplerConfigurationIfDefined(@TempDir Path tempDir) throws IOException {
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

    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    assertThat(model.getTracerProvider().getSampler().getAlwaysOff()).isNotNull();
  }

  @Test
  void shouldCustomizeEmptyConfigurationForSplunkRealm() {
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
                splunk:
                  realm: unreal-test-realm
                  access:
                    token: ABC123456
            """;
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    NameStringValuePairModel tokenHeader =
        new NameStringValuePairModel().withName("X-SF-TOKEN").withValue("ABC123456");
    OtlpHttpExporterModel traceExporterModel =
        model.getTracerProvider().getProcessors().stream()
            .filter(m -> m.getBatch() != null)
            .limit(1)
            .toList()
            .get(0)
            .getBatch()
            .getExporter()
            .getOtlpHttp();
    assertThat(traceExporterModel.getEndpoint())
        .isEqualTo("https://ingest.unreal-test-realm.signalfx.com/v1/traces");
    assertThat(traceExporterModel.getEncoding())
        .isEqualTo(OtlpHttpExporterModel.OtlpHttpEncoding.PROTOBUF);
    assertThat(traceExporterModel.getHeaders()).contains(tokenHeader);

    OtlpHttpMetricExporterModel metricExporterModel =
        model.getMeterProvider().getReaders().get(0).getPeriodic().getExporter().getOtlpHttp();
    assertThat(metricExporterModel.getEndpoint())
        .isEqualTo("https://ingest.unreal-test-realm.signalfx.com/v2/datapoint/otlp");
    assertThat(metricExporterModel.getEncoding())
        .isEqualTo(OtlpHttpExporterModel.OtlpHttpEncoding.PROTOBUF);
    assertThat(metricExporterModel.getHeaders()).contains(tokenHeader);

    OtlpHttpExporterModel logExporterModel =
        model.getLoggerProvider().getProcessors().get(0).getBatch().getExporter().getOtlpHttp();
    assertThat(logExporterModel.getEndpoint()).isEqualTo("http://localhost:4318/v1/logs");
    assertThat(logExporterModel.getEncoding())
        .isEqualTo(OtlpHttpExporterModel.OtlpHttpEncoding.PROTOBUF);
    assertThat(logExporterModel.getHeaders()).contains(tokenHeader);
  }

  @Test
  void shouldNotUpdateAccessTokenProvidedInExporterHeaders() {
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
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

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

  @Test
  void shouldNotUpdateExistingAccessToken() {
    var yaml =
        """
            file_format: "1.0-rc.1"
            log_level: ${TEST_LOG_LEVEL:-crazy}
            """;
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    assertThat(model.getLogLevel()).isEqualTo("crazy");
  }

  private static OpenTelemetryConfigurationModel getCustomizedModel(String yaml) {
    SplunkDeclarativeConfigurationCustomizerProvider customizer =
        new SplunkDeclarativeConfigurationCustomizerProvider();
    return configTestExtension.getCustomizedModel(yaml, customizer);
  }

  private static DeclarativeConfigProperties getCustomizedJavaInstrumentationConfig(String yaml) {
    var model = getCustomizedModel(yaml);
    var configProvider = SdkConfigProvider.create(model);
    return Objects.requireNonNull(configProvider.getInstrumentationConfig()).getStructured("java");
  }
}
