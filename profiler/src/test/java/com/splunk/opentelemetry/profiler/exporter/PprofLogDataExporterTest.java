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

package com.splunk.opentelemetry.profiler.exporter;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import io.opentelemetry.api.common.Value;
import java.nio.charset.StandardCharsets;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PprofLogDataExporterTest {
  @RegisterExtension public final InMemoryOtelLogger logger = new InMemoryOtelLogger();

  @Test
  void emitDataToOpenTelemetryLogger() {
    var logMessage = RandomString.make();

    var exporter =
        new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    assertEquals(Value.of(logMessage), logger.records().get(0).getBodyValue());
  }

  @Test
  void expectedNumberOfLogMessageAttributesAreIncluded() {
    var logMessage = RandomString.make();

    var exporter =
        new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().get(0).getAttributes();
    assertEquals(5, attributes.size());
  }

  @Test
  void includeSplunkSourceTypeAttributeInLogMessage() {
    var logMessage = RandomString.make();

    var exporter =
        new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().get(0).getAttributes();
    assertEquals("otel.profiling", attributes.get(stringKey("com.splunk.sourcetype")));
  }

  @Test
  void includeProfilingDataFormatAttributeInLogMessage() {
    var logMessage = RandomString.make();

    var exporter =
        new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().get(0).getAttributes();
    assertEquals("pprof-gzip-base64", attributes.get(stringKey("profiling.data.format")));
  }

  @ParameterizedTest
  @EnumSource(ProfilingDataType.class)
  void includeProfilingDataTypeAttributeInLogMessage(ProfilingDataType dataType) {
    var logMessage = RandomString.make();

    var exporter = new PprofLogDataExporter(logger, dataType, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().get(0).getAttributes();
    assertEquals(dataType.value(), attributes.get(stringKey("profiling.data.type")));
  }

  @ParameterizedTest
  @EnumSource(InstrumentationSource.class)
  void includeProfilingInstrumentationSourceAttributeInLogMessage(InstrumentationSource source) {
    var logMessage = RandomString.make();

    var exporter = new PprofLogDataExporter(logger, ProfilingDataType.CPU, source);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().get(0).getAttributes();
    assertEquals(source.value(), attributes.get(stringKey("profiling.instrumentation.source")));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 50, 100, 10_000})
  void includeFrameCountAttributeInLogMessage(int frameCount) {
    var logMessage = RandomString.make();

    var exporter =
        new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), frameCount);

    var attributes = logger.records().get(0).getAttributes();
    assertEquals(frameCount, attributes.get(longKey("profiling.data.total.frame.count")));
  }
}
