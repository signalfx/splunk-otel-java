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

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.parseAndCustomizeModel;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ProfilerConfigurationCustomizerProviderTest {
  @Test
  void testCustomizer() {
    try (MockedStatic<JFR> jfrMock = mockStatic(JFR.class);
        MockedStatic<ContextStorage> contextStorageMock = mockStatic(ContextStorage.class)) {
      // given
      var yaml =
          """
              file_format: "1.0-rc.1"
              instrumentation/development:
                java:
                  splunk:
                     profiler:
                       enabled: true
              """;
      var jfrInstanceMock = mock(JFR.class);
      when(jfrInstanceMock.isAvailable()).thenReturn(true);
      jfrMock.when(JFR::getInstance).thenReturn(jfrInstanceMock);

      var testedCustomizer = new ProfilerConfigurationCustomizerProvider();

      // when
      OpenTelemetryConfigurationModel model = parseAndCustomizeModel(yaml, testedCustomizer);

      // then
      assertThat(model).isNotNull();
      contextStorageMock.verify(() -> ContextStorage.addWrapper(any()));
    }
  }
}
