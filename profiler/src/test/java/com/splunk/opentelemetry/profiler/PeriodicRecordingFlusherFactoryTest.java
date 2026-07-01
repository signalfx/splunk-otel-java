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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.export.HttpSenderProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

class PeriodicRecordingFlusherFactoryTest {

  @TempDir Path tempDir;

  @AfterEach
  void tearDown() {
    ProfilerConfiguration.SUPPLIER.reset();
  }

  @Test
  void createConfiguresJfrAndWiresRecorderIntoFlusher() {
    JFR jfr = mock(JFR.class);
    ProfilerConfiguration config =
        config(tempDir).setStackDepth(73).setRecordingDuration(Duration.ofMillis(100)).build();
    ProfilerConfiguration.SUPPLIER.configure(config);
    PeriodicRecordingFlusherFactory factory = new PeriodicRecordingFlusherFactory();

    try (MockedConstruction<JfrRecorder> recorderConstruction =
        mockConstruction(JfrRecorder.class)) {
      PeriodicRecordingFlusher flusher = factory.create(config, Resource.empty(), jfr);

      assertThat(flusher).isNotNull();
      assertThat(recorderConstruction.constructed()).hasSize(1);
      verify(jfr).setStackDepth(73);

      JfrRecorder recorder = recorderConstruction.constructed().get(0);
      when(recorder.isStarted()).thenReturn(true);

      flusher.handleInterval();

      verify(recorder).flushSnapshot();
    }
  }

  @Test
  void createCreatesMissingOutputDirectoryWhenKeepingFiles() {
    Path outputDir = tempDir.resolve("profiler-output");
    JFR jfr = mock(JFR.class);
    ProfilerConfiguration config = config(outputDir).setKeepFiles(true).build();
    ProfilerConfiguration.SUPPLIER.configure(config);
    PeriodicRecordingFlusherFactory factory = new PeriodicRecordingFlusherFactory();

    try (MockedConstruction<JfrRecorder> recorderConstruction =
        mockConstruction(JfrRecorder.class)) {
      PeriodicRecordingFlusher flusher = factory.create(config, Resource.empty(), jfr);

      assertThat(flusher).isNotNull();
      assertThat(outputDir).isDirectory();
      assertThat(recorderConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void createContinuesWhenKeepFilesPathIsNotADirectory() throws Exception {
    Path outputFile = tempDir.resolve("profiler-output");
    Files.createFile(outputFile);
    JFR jfr = mock(JFR.class);
    ProfilerConfiguration config = config(outputFile).setKeepFiles(true).build();
    ProfilerConfiguration.SUPPLIER.configure(config);
    PeriodicRecordingFlusherFactory factory = new PeriodicRecordingFlusherFactory();

    try (MockedConstruction<JfrRecorder> recorderConstruction =
        mockConstruction(JfrRecorder.class)) {
      PeriodicRecordingFlusher flusher = factory.create(config, Resource.empty(), jfr);

      assertThat(flusher).isNotNull();
      assertThat(recorderConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void buildUsesProfilerConfigComponentLoaderWhenExporterNodeIsAbsent() {
    RecordingComponentLoader componentLoader = new RecordingComponentLoader();
    DeclarativeConfigProperties configProperties =
        YamlDeclarativeConfigProperties.create(Collections.emptyMap(), componentLoader);
    JFR jfr = mock(JFR.class);
    ProfilerConfiguration config = config(tempDir).setConfigProperties(configProperties).build();
    ProfilerConfiguration.SUPPLIER.configure(config);
    PeriodicRecordingFlusherFactory factory = new PeriodicRecordingFlusherFactory();

    try (MockedConstruction<JfrRecorder> recorderConstruction =
        mockConstruction(JfrRecorder.class)) {
      PeriodicRecordingFlusher flusher = factory.create(config, Resource.empty(), jfr);

      assertThat(flusher).isNotNull();
      assertThat(recorderConstruction.constructed()).hasSize(1);
      assertThat(componentLoader.loaded(HttpSenderProvider.class)).isTrue();
    }
  }

  private ProfilerConfiguration.Builder config(Path outputDir) {
    return ProfilerConfiguration.builder()
        .setEnabled(true)
        .setIngestUrl("http://localhost:4318/v1/logs")
        .setOtlpProtocol("http/protobuf")
        .setProfilerDirectory(outputDir.toString())
        .setRecordingDuration(Duration.ofDays(1))
        .setConfigProperties(
            DefaultConfigProperties.createFromMap(
                Map.of(
                    "otel.exporter.otlp.protocol",
                    "http/protobuf",
                    "otel.exporter.otlp.endpoint",
                    "http://localhost:4318")));
  }

  private static class RecordingComponentLoader implements ComponentLoader {
    private final ComponentLoader delegate =
        ComponentLoader.forClassLoader(PeriodicRecordingFlusherFactoryTest.class.getClassLoader());
    private final List<Class<?>> loadedSpiClasses = new ArrayList<>();

    @Override
    public <T> Iterable<T> load(Class<T> spiClass) {
      loadedSpiClasses.add(spiClass);
      return delegate.load(spiClass);
    }

    boolean loaded(Class<?> spiClass) {
      return loadedSpiClasses.contains(spiClass);
    }
  }
}
