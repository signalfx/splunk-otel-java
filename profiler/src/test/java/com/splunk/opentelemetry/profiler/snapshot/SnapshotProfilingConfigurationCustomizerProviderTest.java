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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class SnapshotProfilingConfigurationCustomizerProviderTest {
  @RegisterExtension final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @AfterEach
  void resetSuppliers() {
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.reset();
    StackTraceSampler.SUPPLIER.reset();
    StagingArea.SUPPLIER.reset();
    SpanTracker.SUPPLIER.reset();
  }

  @Test
  void shouldDoNothingIfProfilerIsNotEnabled(@TempDir Path tempDir) throws IOException {
    // given
    String yaml =
        """
          file_format: "1.0-rc.3"
          """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model).isNotNull();
    assertThat(model.getPropagator()).isNull();
    assertThat(model.getTracerProvider()).isNull();
  }

  @Test
  void shouldAddRequiredPropagator() {
    // given
    String yaml =
        """
          file_format: "1.0-rc.3"
          distribution:
            splunk:
              profiling:
                callgraphs:
          """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    assertThat(model.getPropagator().getCompositeList()).isEqualTo("splunk_snapshot_volume");
  }

  @Test
  void shouldKeepPropagatorsDefinedInCompositeList() {
    // given
    String yaml =
        """
          file_format: "1.0-rc.3"
          propagator:
            composite_list: "propagator1,propagator2"
          distribution:
            splunk:
              profiling:
                callgraphs:
          """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    Set<String> propagators = Set.of(model.getPropagator().getCompositeList().split(","));
    assertThat(propagators)
        .isEqualTo(Set.of("propagator1", "propagator2", "splunk_snapshot_volume"));
  }

  @Test
  void shouldAddSpanProcessors() {
    // given
    String yaml =
        """
          file_format: "1.0-rc.3"
          tracer_provider:
            processors:
              - batch:
              - simple:
          distribution:
            splunk:
              profiling:
                callgraphs:
          """;

    // when
    OpenTelemetryConfigurationModel model = getCustomizedModel(yaml);

    // then
    List<SpanProcessorModel> expectedProcessors =
        List.of(
            new SpanProcessorModel().withBatch(new BatchSpanProcessorModel()),
            new SpanProcessorModel().withSimple(new SimpleSpanProcessorModel()),
            new SpanProcessorModel()
                .withAdditionalProperty(SnapshotProfilingSpanProcessorComponentProvider.NAME, null),
            new SpanProcessorModel()
                .withAdditionalProperty(SdkShutdownHookComponentProvider.NAME, null));

    assertThat(model).isNotNull();
    assertThat(model.getTracerProvider()).isNotNull();
    assertThat(model.getTracerProvider().getProcessors()).hasSize(4);
    assertThat(model.getTracerProvider().getProcessors()).containsAll(expectedProcessors);
  }

  @Test
  void shouldInitializeActiveSpansTracking() {
    // given
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            """
              file_format: "1.0-rc.3"
              distribution:
                splunk:
                  profiling:
                    callgraphs:
              """);

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
