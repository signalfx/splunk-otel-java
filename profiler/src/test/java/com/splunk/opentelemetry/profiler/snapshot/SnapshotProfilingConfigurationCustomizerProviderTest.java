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

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
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
        toYamlString("file_format: \"1.0-rc.1\"", "instrumentation/development:", "  java:");

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model).isNotNull();
    assertThat(model.getPropagator()).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldCustomizePropagators() {
    // given
    String yaml =
        toYamlString(
            "file_format: \"1.0-rc.1\"",
            "instrumentation/development:",
            "  java:",
            "    splunk:",
            "      profiler:",
            "        enabled: true");

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model).isNotNull();
    assertThat(model.getPropagator()).isNotNull();
    assertThat(model.getPropagator().getComposite()).hasSize(3);
    assertThat(model.getPropagator().getComposite().get(0).getTracecontext()).isNotNull();
    assertThat(model.getPropagator().getComposite().get(1).getBaggage()).isNotNull();
    Map<String, Object> propagatorProperties =
        model.getPropagator().getComposite().get(2).getAdditionalProperties();
    assertThat(propagatorProperties).isNotNull();
    Map<String, Object> snapshotPropagatorProperties =
        (Map<String, Object>) propagatorProperties.get("splunk-snapshot");
    assertThat(snapshotPropagatorProperties).isNotNull();
    assertThat(snapshotPropagatorProperties.get("splunk"))
        .isNotNull(); // Copied java instrumentation config
  }

  @Test
  void shouldNotAddTracecontextIfSomePropagatorsPredefinedInConfig() {
    // given
    String yaml =
        toYamlString(
            "file_format: \"1.0-rc.1\"",
            "propagator:",
            "  composite_list: some-propagator",
            "instrumentation/development:",
            "  java:",
            "    splunk:",
            "      profiler:",
            "        enabled: true");

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model).isNotNull();
    assertThat(model.getPropagator()).isNotNull();
    assertThat(model.getPropagator().getComposite()).hasSize(2);
    assertThat(model.getPropagator().getComposite().get(0).getBaggage()).isNotNull();
    assertThat(model.getPropagator().getComposite().get(1).getAdditionalProperties()).isNotNull();
  }

  @Test
  void shouldAddSnapshotSpanProcessorAndShutdownHookSpanProcessor() {
    // given
    String yaml =
        toYamlString(
            "file_format: \"1.0-rc.1\"",
            "instrumentation/development:",
            "  java:",
            "    splunk:",
            "      profiler:",
            "        enabled: true");

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model).isNotNull();
    assertThat(model.getTracerProvider()).isNotNull();
    assertThat(model.getTracerProvider().getProcessors()).hasSize(2);
    assertThat(model.getTracerProvider().getProcessors())
        .extracting(processor -> processor.getAdditionalProperties().keySet())
        .containsOnlyOnceElementsOf(
            Arrays.asList(Sets.set("splunk-snapshot-profiling"), Sets.set("sdk-shutdown-hook")));
  }

  private static OpenTelemetryConfigurationModel getCustomizedModel(String yaml) {
    SnapshotProfilingConfigurationCustomizerProvider customizer =
        new SnapshotProfilingConfigurationCustomizerProvider();
    return DeclarativeConfigTestUtil.parseAndCustomizeModel(yaml, customizer);
  }
}
