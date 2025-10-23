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

package com.splunk.opentelemetry.webengine;

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.parseAndCustomizeModel;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import org.junit.jupiter.api.Test;

class WebengineSpanProcessorCustomizerProviderTest {

  @Test
  void shouldAddWebengineSpanProcessor() {
    String yaml =
        """
            file_format: "1.0-rc.1"
            tracer_provider:
              processors:
                - simple:
                    exporter:
                      console:
            """;
    WebengineSpanProcessorCustomizerProvider customizer =
        new WebengineSpanProcessorCustomizerProvider();

    OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, customizer);

    var processors = model.getTracerProvider().getProcessors();
    assertThat(processors.size()).isEqualTo(2);
    assertThat(
            processors
                .get(0)
                .getAdditionalProperties()
                .containsKey(WebengineSpanProcessorComponentProvider.PROVIDER_NAME))
        .isTrue();
    assertThat(processors.get(1).getSimple()).isNotNull();
  }
}
