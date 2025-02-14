package com.splunk.opentelemetry.profiler.exporter;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import io.opentelemetry.api.common.Value;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PprofLogDataExporterTest {
  @RegisterExtension public final InMemoryOtelLogger logger = new InMemoryOtelLogger();

  @Test
  void emitDataToOpenTelemetryLogger() {
    var logMessage = "this is a test log message, the contents are not important.";

    var exporter = new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    assertEquals(Value.of(logMessage), logger.records().getFirst().getBodyValue());
  }

  @Test
  void includeSplunkSourceTypeAttributeInLogMessage() {
    var logMessage = "this is a test log message, the contents are not important.";

    var exporter = new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().getFirst().getAttributes();
    assertEquals("otel.profiling", attributes.get(stringKey("com.splunk.sourcetype")));
  }

  @Test
  void includeProfilingDataFormatAttributeInLogMessage() {
    var logMessage = "this is a test log message, the contents are not important.";

    var exporter = new PprofLogDataExporter(logger, ProfilingDataType.CPU, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().getFirst().getAttributes();
    assertEquals("pprof-gzip-base64", attributes.get(stringKey("profiling.data.format")));
  }

  @ParameterizedTest
  @EnumSource(ProfilingDataType.class)
  void includeProfilingDataTypeAttributeInLogMessage(ProfilingDataType dataType) {
    var logMessage = "this is a test log message, the contents are not important.";

    var exporter = new PprofLogDataExporter(logger, dataType, InstrumentationSource.CONTINUOUS);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().getFirst().getAttributes();
    assertEquals(dataType.value(), attributes.get(stringKey("profiling.data.type")));
  }

  @ParameterizedTest
  @EnumSource(InstrumentationSource.class)
  void includeProfilingInstrumentationSourceAttributeInLogMessage(InstrumentationSource source) {
    var logMessage = "this is a test log message, the contents are not important.";

    var exporter = new PprofLogDataExporter(logger, ProfilingDataType.CPU, source);
    exporter.export(logMessage.getBytes(StandardCharsets.UTF_8), 1);

    var attributes = logger.records().getFirst().getAttributes();
    assertEquals(source.value(), attributes.get(stringKey("profiling.instrumentation.source")));
  }
}
