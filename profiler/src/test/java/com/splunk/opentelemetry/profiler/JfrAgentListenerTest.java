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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JfrAgentListenerTest {
  @RegisterExtension static final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @AfterEach
  void resetDeclarativeConfigSuppliers() {
    ProfilerConfiguration.SUPPLIER.reset();
    SnapshotProfilingConfiguration.SUPPLIER.reset();
  }

  @Test
  void shouldActivateJfrRecording(@TempDir Path tempDir) throws IOException {
    // given
    String yaml =
        """
        file_format: "1.1"
        distribution:
          splunk:
            profiling:
              always_on:
        """;
    AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

    var jfr = mock(JFR.class);
    var supervisor = mock(ProfilingSupervisor.class);

    when(jfr.isAvailable()).thenReturn(true);

    JfrAgentListener listener =
        new JfrAgentListener(jfr) {
          @Override
          ProfilingSupervisor makeProfilingSupervisor(AutoConfiguredOpenTelemetrySdk sdk) {
            return supervisor;
          }
        };

    // when
    listener.afterAgent(sdk);

    // then
    verify(supervisor).requestStartProfiling();
  }

  @Test
  void shouldNotActivateJfrRecording_JfrNotAvailable(@TempDir Path tempDir) throws IOException {

    // given
    String yaml =
        """
        file_format: "1.1"
        distribution:
          splunk:
            profiling:
              always_on:
        """;
    AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

    var supervisor = mock(ProfilingSupervisor.class);
    var jfr = mock(JFR.class);
    when(jfr.isAvailable()).thenReturn(false);

    JfrAgentListener listener =
        new JfrAgentListener(jfr) {
          @Override
          ProfilingSupervisor makeProfilingSupervisor(AutoConfiguredOpenTelemetrySdk sdk) {
            return supervisor;
          }
        };

    // when
    listener.afterAgent(sdk);

    // then
    verifyNoInteractions(supervisor);
  }

  @ParameterizedTest
  @MethodSource("generateNoProfilerYamlStrings")
  void shouldNotActivateJfrRecording_profilerDisabled(String yaml, @TempDir Path tempDir)
      throws IOException {

    // given
    AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

    var jfr = mock(JFR.class);
    var supervisor = mock(ProfilingSupervisor.class);
    when(jfr.isAvailable()).thenReturn(true);

    JfrAgentListener listener =
        new JfrAgentListener(jfr) {
          @Override
          ProfilingSupervisor makeProfilingSupervisor(AutoConfiguredOpenTelemetrySdk sdk) {
            return supervisor;
          }
        };

    // when
    listener.afterAgent(sdk);

    // then
    verifyNoInteractions(supervisor);
  }

  private List<Arguments> generateNoProfilerYamlStrings() {
    return List.of(
        Arguments.of("file_format: \"1.1\""),
        Arguments.of(
            """
        file_format: "1.1"
        distribution:
        """),
        Arguments.of(
            """
        file_format: "1.1"
        distribution:
          splunk:
        """),
        Arguments.of(
            """
        file_format: "1.1"
        distribution:
          splunk:
            something:
        """));
  }
}
