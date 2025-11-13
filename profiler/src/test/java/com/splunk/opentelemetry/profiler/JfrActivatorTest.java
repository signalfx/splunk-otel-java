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
    try (MockedStatic<JFR> jfrMock = mockStatic(JFR.class);
        MockedStatic<ContextStorage> contextStorageMock = mockStatic(ContextStorage.class)) {

      // given
      String yaml = toYamlString(
          "file_format: \"1.0-rc.1\"",
          "instrumentation/development:",
          "  java:",
          "    splunk:",
          "      profiler:",
          "        enabled: true"
      );
      AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir);

      var jfrInstanceMock = mock(JFR.class);
      when(jfrInstanceMock.isAvailable()).thenReturn(true);
      jfrMock.when(JFR::getInstance).thenReturn(jfrInstanceMock);

      ExecutorService executorMock = mock(ExecutorService.class);
      JfrActivator activator = new JfrActivator(executorMock);

      // when
      activator.afterAgent(sdk);

      // then
      contextStorageMock.verify(() -> ContextStorage.addWrapper(any()));
      verify(executorMock).submit(any(Runnable.class));
    }
  }

  @Test
  void shouldNotActivateJfrRecording_JfrNotAvailable(@TempDir Path tempDir) throws IOException {
    try (MockedStatic<JFR> jfrMock = mockStatic(JFR.class);
        MockedStatic<ContextStorage> contextStorageMock = mockStatic(ContextStorage.class)) {

      // given
      String yaml = toYamlString(
          "file_format: \"1.0-rc.1\"",
          "instrumentation/development:",
          "  java:",
          "    splunk:",
          "      profiler:",
          "        enabled: true"
      );
      AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir);

      var jfrInstanceMock = mock(JFR.class);
      when(jfrInstanceMock.isAvailable()).thenReturn(false);
      jfrMock.when(JFR::getInstance).thenReturn(jfrInstanceMock);

      ExecutorService executorMock = mock(ExecutorService.class);
      JfrActivator activator = new JfrActivator(executorMock);

      // when
      activator.afterAgent(sdk);

      // then
      contextStorageMock.verifyNoInteractions();
      verifyNoInteractions(executorMock);
    }
  }

  @Test
  void shouldNotActivateJfrRecording_profilerDisabled(@TempDir Path tempDir) throws IOException {
    try (MockedStatic<JFR> jfrMock = mockStatic(JFR.class);
        MockedStatic<ContextStorage> contextStorageMock = mockStatic(ContextStorage.class)) {

      // given
      String yaml = toYamlString(
          "file_format: \"1.0-rc.1\"",
          "instrumentation/development:",
          "  java:"
      );
      AutoConfiguredOpenTelemetrySdk sdk = createAutoConfiguredSdk(yaml, tempDir);

      var jfrInstanceMock = mock(JFR.class);
      when(jfrInstanceMock.isAvailable()).thenReturn(true);
      jfrMock.when(JFR::getInstance).thenReturn(jfrInstanceMock);

      ExecutorService executorMock = mock(ExecutorService.class);
      JfrActivator activator = new JfrActivator(executorMock);

      // when
      activator.afterAgent(sdk);

      // then
      contextStorageMock.verifyNoInteractions();
      verifyNoInteractions(executorMock);
    }
  }
}
