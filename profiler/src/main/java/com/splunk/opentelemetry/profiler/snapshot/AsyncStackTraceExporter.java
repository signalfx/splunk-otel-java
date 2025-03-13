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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.FRAME_COUNT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.INSTRUMENTATION_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PPROF_GZIP_BASE64;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;

import com.google.common.annotations.VisibleForTesting;
import com.google.perftools.profiles.ProfileProto.Profile;
import com.splunk.opentelemetry.profiler.InstrumentationSource;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.ImmutableSpanContext;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

class AsyncStackTraceExporter implements StackTraceExporter {
  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(AsyncStackTraceExporter.class.getName());

  private static final Attributes COMMON_ATTRIBUTES =
      Attributes.builder()
          .put(SOURCE_TYPE, PROFILING_SOURCE)
          .put(DATA_TYPE, ProfilingDataType.CPU.value())
          .put(DATA_FORMAT, PPROF_GZIP_BASE64)
          .put(INSTRUMENTATION_SOURCE, InstrumentationSource.SNAPSHOT.value())
          .build();

  private final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private final PprofTranslator translator = new PprofTranslator();
  private final Logger otelLogger;
  private final Clock clock;

  AsyncStackTraceExporter(Logger logger) {
    this(logger, Clock.systemUTC());
  }

  @VisibleForTesting
  AsyncStackTraceExporter(Logger logger, Clock clock) {
    this.otelLogger = logger;
    this.clock = clock;
  }

  @Override
  public void export(List<StackTrace> stackTraces) {
    executor.submit(pprofExporter(otelLogger, stackTraces));
  }

  private Runnable pprofExporter(Logger otelLogger, List<StackTrace> stackTraces) {
    return () -> {
      try {
        Context context = createProfilingContext(stackTraces);
        Pprof pprof = translator.toPprof(stackTraces);
        Profile profile = pprof.build();
        otelLogger
            .logRecordBuilder()
            .setContext(context)
            .setTimestamp(Instant.now(clock))
            .setSeverity(Severity.INFO)
            .setAllAttributes(profilingAttributes(pprof))
            .setBody(serialize(profile))
            .emit();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "an exception was thrown", e);
      }
    };
  }

  private Context createProfilingContext(List<StackTrace> stackTraces) {
    String traceId = extractTraceId(stackTraces);
    SpanContext spanContext =
        ImmutableSpanContext.create(
            traceId,
            SpanId.getInvalid(),
            TraceFlags.getDefault(),
            TraceState.getDefault(),
            false,
            true);
    Span span = Span.wrap(spanContext);
    return span.storeInContext(Context.root());
  }

  private String extractTraceId(List<StackTrace> stackTraces) {
    return stackTraces.stream().findFirst().map(StackTrace::getTraceId).orElse(null);
  }

  private Attributes profilingAttributes(Pprof pprof) {
    return COMMON_ATTRIBUTES.toBuilder().put(FRAME_COUNT, pprof.frameCount()).build();
  }

  private String serialize(Profile profile) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    try (OutputStream outputStream = new GZIPOutputStream(Base64.getEncoder().wrap(byteStream))) {
      profile.writeTo(outputStream);
    }
    return byteStream.toString(StandardCharsets.ISO_8859_1.name());
  }
}
