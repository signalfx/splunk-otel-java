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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.FRAME_COUNT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PPROF_GZIP_BASE64;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static java.util.logging.Level.FINE;

import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import java.nio.charset.StandardCharsets;

public class PprofLogDataExporter {
  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(PprofLogDataExporter.class.getName());

  private final Logger otelLogger;
  private final ProfilingDataType dataType;
  private final Attributes commonAttributes;

  public PprofLogDataExporter(Logger otelLogger, ProfilingDataType dataType, InstrumentationSource instrumentationSource) {
    this.otelLogger = otelLogger;
    this.dataType = dataType;
    this.commonAttributes =
        Attributes.builder()
            .put(SOURCE_TYPE, PROFILING_SOURCE)
            .put(DATA_TYPE, dataType.value())
            .put(DATA_FORMAT, PPROF_GZIP_BASE64)
            .put("profiling.instrumentation.source", instrumentationSource.value())
            .build();
  }

  public void export(byte[] bytes, int frameCount) {
    if (logger.isLoggable(FINE)) {
      logger.log(
          FINE,
          "Exporting {0} data as pprof, size {1}.",
          new Object[] {dataType.value(), bytes.length});
    }

    String body = new String(bytes, StandardCharsets.ISO_8859_1);
    Attributes attributes = commonAttributes.toBuilder().put(FRAME_COUNT, frameCount).build();
    otelLogger.logRecordBuilder().setBody(body).setAllAttributes(attributes).emit();
  }
}
