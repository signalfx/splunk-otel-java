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

package com.splunk.opentelemetry.profiler.snapshot;

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.toYamlString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.IOException;
import java.nio.file.Path;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class SnapshotProfilingConfigurationCustomizerProviderTest {
  @RegisterExtension final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @Test
  void shouldDoNothingIfProfilerIsNotEnabled(@TempDir Path tempDir) throws IOException {
    // given
    String yaml =
        toYamlString("file_format: \"1.0-rc.3\"", "instrumentation/development:", "  java:");

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model).isNotNull();
    assertThat(model.getPropagator()).isNull();
  }

  @Test
  void shouldAddShutdownHookSpanProcessor() {
    // given
    String yaml =
        toYamlString(
            "file_format: \"1.0-rc.3\"",
            "instrumentation/development:",
            "  java:",
            "    distribution:",
            "      splunk:",
            "        profiling:",
            "          callgraphs:");

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model).isNotNull();
    assertThat(model.getTracerProvider()).isNotNull();
    assertThat(model.getTracerProvider().getProcessors()).hasSize(1);
    assertThat(model.getTracerProvider().getProcessors())
        .extracting(processor -> processor.getAdditionalProperties().keySet())
        .containsOnlyOnce(Sets.set("sdk_shutdown_hook"));
  }

  @Test
  void shouldInitialize() {
    // given
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            "file_format: \"1.0-rc.3\"",
            "instrumentation/development:",
            "  java:",
            "    distribution:",
            "      splunk:",
            "        profiling:",
            "          callgraphs:");

    TraceRegistry traceRegistryMock = mock(TraceRegistry.class);
    ContextStorageWrapper contextStorageWrapperMock = mock(ContextStorageWrapper.class);

    SnapshotProfilingConfigurationCustomizerProvider customizerProvider =
        new SnapshotProfilingConfigurationCustomizerProvider(
            traceRegistryMock, contextStorageWrapperMock);

    // when
    customizerProvider.customizeModel(model);

    // then
    verify(contextStorageWrapperMock).wrapContextStorage(traceRegistryMock);
  }

  private static OpenTelemetryConfigurationModel getCustomizedModel(String yaml) {
    SnapshotProfilingConfigurationCustomizerProvider customizer =
        new SnapshotProfilingConfigurationCustomizerProvider();
    return DeclarativeConfigTestUtil.parseAndCustomizeModel(yaml, customizer);
  }
}
