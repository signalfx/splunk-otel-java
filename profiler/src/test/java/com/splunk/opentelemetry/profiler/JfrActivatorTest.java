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

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.createAutoConfiguredSdk;
import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.toYamlString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class JfrActivatorTest {
  @Test
  void shouldActivateJfrRecording(@TempDir Path tempDir) throws IOException {
    try (MockedStatic<ContextStorage> contextStorageMock = mockStatic(ContextStorage.class)) {

      // given
      String yaml =
          """
            file_format: "1.0-rc.3"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      always_on:
            """;
      AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir);

      var jfrMock = mock(JFR.class);
      when(jfrMock.isAvailable()).thenReturn(true);

      ExecutorService executorMock = mock(ExecutorService.class);
      JfrActivator activator = new JfrActivator(jfrMock, executorMock);

      // when
      activator.afterAgent(sdk);

      // then
      contextStorageMock.verify(() -> ContextStorage.addWrapper(any()));
      verify(executorMock).submit(any(Runnable.class));
    }
  }

  @Test
  void shouldNotActivateJfrRecording_JfrNotAvailable(@TempDir Path tempDir) throws IOException {
    try (MockedStatic<ContextStorage> contextStorageMock = mockStatic(ContextStorage.class)) {

      // given
      String yaml =
          """
            file_format: "1.0-rc.3"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      always_on:
            """;
      AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir);

      var jfrMock = mock(JFR.class);
      when(jfrMock.isAvailable()).thenReturn(false);

      ExecutorService executorMock = mock(ExecutorService.class);
      JfrActivator activator = new JfrActivator(jfrMock, executorMock);

      // when
      activator.afterAgent(sdk);

      // then
      contextStorageMock.verifyNoInteractions();
      verifyNoInteractions(executorMock);
    }
  }

  @Test
  void shouldNotActivateJfrRecording_profilerDisabled(@TempDir Path tempDir) throws IOException {
    try (MockedStatic<ContextStorage> contextStorageMock = mockStatic(ContextStorage.class)) {

      // given
      String yaml =
          toYamlString("file_format: \"1.0-rc.3\"", "instrumentation/development:", "  java:");
      AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir);

      var jfrMock = mock(JFR.class);
      when(jfrMock.isAvailable()).thenReturn(true);

      ExecutorService executorMock = mock(ExecutorService.class);
      JfrActivator activator = new JfrActivator(jfrMock, executorMock);

      // when
      activator.afterAgent(sdk);

      // then
      contextStorageMock.verifyNoInteractions();
      verifyNoInteractions(executorMock);
    }
  }
}
