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
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.INSTRUMENTATION_LIBRARY_INFO;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;

import com.splunk.opentelemetry.profiler.Configuration.DataFormat;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PprofLogDataExporter {
  private static final Logger logger = LoggerFactory.getLogger(PprofLogDataExporter.class);

  private final LogProcessor logProcessor;
  private final Resource resource;
  private final ProfilingDataType dataType;
  private final DataFormat dataFormat;
  private final Attributes commonAttributes;

  public PprofLogDataExporter(
      LogProcessor logProcessor,
      Resource resource,
      ProfilingDataType dataType,
      DataFormat dataFormat) {
    this.logProcessor = logProcessor;
    this.resource = resource;
    this.dataType = dataType;
    this.dataFormat = dataFormat;
    this.commonAttributes =
        Attributes.builder()
            .put(SOURCE_TYPE, PROFILING_SOURCE)
            .put(DATA_TYPE, dataType.value())
            .put(DATA_FORMAT, dataFormat.value())
            .build();
  }

  public void export(byte[] bytes) {
    logger.debug(
        "Exporting {} data as {}, size {}.", dataType.value(), dataFormat.value(), bytes.length);

    String body = new String(bytes, StandardCharsets.ISO_8859_1);
    LogDataBuilder logDataBuilder =
        LogDataBuilder.create(resource, INSTRUMENTATION_LIBRARY_INFO)
            .setEpoch(Instant.now())
            .setBody(body)
            .setAttributes(commonAttributes);

    logProcessor.emit(logDataBuilder.build());
  }
}
