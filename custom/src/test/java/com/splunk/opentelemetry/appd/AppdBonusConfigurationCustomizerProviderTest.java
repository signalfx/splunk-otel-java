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

package com.splunk.opentelemetry.appd;

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.parseAndCustomizeModel;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import org.junit.jupiter.api.Test;

class AppdBonusConfigurationCustomizerProviderTest {

  @Test
  void shouldAddAppdPropagatorWithDefaultPropagatorsAndSpanProcessorWhenFeatureIsEnabled() {
    var yaml =
        """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                cisco:
                   ctx:
                     enabled: true
            """;

    AppdBonusConfigurationCustomizerProvider customizer =
        new AppdBonusConfigurationCustomizerProvider();
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    assertThat(model.getPropagator().getCompositeList())
        .isEqualTo("appd-bonus,tracecontext,baggage");
    assertThat(model.getTracerProvider().getProcessors()).hasSize(1);
    assertThat(model.getTracerProvider().getProcessors().get(0).getAdditionalProperties())
        .hasSize(1);
    assertThat(model.getTracerProvider().getProcessors().get(0).getAdditionalProperties())
        .containsKey("appd-bonus");
  }

  @Test
  void shouldAddAppdPropagatorToExistingListAndSpanProcessorWhenFeatureIsEnabled() {
    var yaml =
        """
            file_format: "1.0-rc.2"
            propagator:
               composite_list: "b3"
            instrumentation/development:
              java:
                cisco:
                   ctx:
                     enabled: true
            """;

    AppdBonusConfigurationCustomizerProvider customizer =
        new AppdBonusConfigurationCustomizerProvider();
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    assertThat(model.getPropagator().getCompositeList()).isEqualTo("appd-bonus,b3");
    assertThat(model.getTracerProvider().getProcessors()).hasSize(1);
    assertThat(model.getTracerProvider().getProcessors().get(0).getAdditionalProperties())
        .hasSize(1);
    assertThat(model.getTracerProvider().getProcessors().get(0).getAdditionalProperties())
        .containsKey("appd-bonus");
  }

  @Test
  void shouldNotAddPropagatorAndSpanProcessorWhenFeatureIsDisabled() {
    var yaml =
        """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                cisco:
                   ctx:
                     enabled: false
            """;

    AppdBonusConfigurationCustomizerProvider customizer =
        new AppdBonusConfigurationCustomizerProvider();
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    assertThat(model.getPropagator()).isNull();
    assertThat(model.getTracerProvider()).isNull();
  }

  @Test
  void shouldNotAddPropagatorAndSpanProcessorWhenFeaturePropertyIsMissing() {
    var yaml =
        """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
            """;

    AppdBonusConfigurationCustomizerProvider customizer =
        new AppdBonusConfigurationCustomizerProvider();
    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    assertThat(model.getPropagator()).isNull();
    assertThat(model.getTracerProvider()).isNull();
  }
}
