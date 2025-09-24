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

import static com.splunk.opentelemetry.DeclarativeConfigTestUtil.parseAndCustomizeModel;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanLimitsModel;
import org.junit.jupiter.api.Test;

class SplunkSpanLimitsCustomizerProviderTest {
  @Test
  void shouldNotAddLimitsIfNoTracerProviderDefined() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            instrumentation/development:
              java:
            """;

    SplunkSpanLimitsCustomizerProvider customizer = new SplunkSpanLimitsCustomizerProvider();

    // when
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    // then
    assertThat(model.getTracerProvider()).isNull();
  }

  @Test
  void shouldNotAddLimitsToTracerProviderIfDefinedInYAML() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              limits:
                  attribute_value_length_limit: 1410
                  attribute_count_limit: 33
              processors:
                - simple:
                    exporter:
                      console:
            instrumentation/development:
              java:
            """;

    SplunkSpanLimitsCustomizerProvider customizer = new SplunkSpanLimitsCustomizerProvider();

    // when
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    // then
    var expectedLimitsModel =
        new SpanLimitsModel().withAttributeValueLengthLimit(1410).withAttributeCountLimit(33);

    assertThat(model.getTracerProvider()).isNotNull();
    assertThat(model.getTracerProvider().getLimits()).isEqualTo(expectedLimitsModel);
  }

  @Test
  void shouldAddLimitsToTracerProvider() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              processors:
                - simple:
                    exporter:
                      console:
            instrumentation/development:
              java:
            """;

    SplunkSpanLimitsCustomizerProvider customizer = new SplunkSpanLimitsCustomizerProvider();

    // when
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    // then
    assertThat(model.getTracerProvider()).isNotNull();
  }
}
