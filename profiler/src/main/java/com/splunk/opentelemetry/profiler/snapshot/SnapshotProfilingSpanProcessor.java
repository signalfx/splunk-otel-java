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

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.logging.Logger;

public class SnapshotProfilingSpanProcessor implements SpanProcessor {
  private static final Logger logger = Logger.getLogger(SnapshotProfilingSpanProcessor.class.getName());
  private final TraceRegistry registry;

  SnapshotProfilingSpanProcessor(TraceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void onStart(Context context, ReadWriteSpan span) {
    logger.info(span.toString());
    Volume volume = Volume.fromString(Baggage.fromContext(context).getEntryValue("splunk.trace.snapshot.volume"));
    logger.info("Volume: " + volume);
    if (volume == Volume.HIGHEST) {
      logger.info("Snapshot volume is highest, registering trace for profiling");
      registry.register(span.getSpanContext());
    }
//    if (isRoot(span) && isEntry(span)) {
//      registry.register(span.getSpanContext());
//    }
  }

  private boolean isRoot(ReadableSpan span) {
    return !span.getParentSpanContext().isValid();
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  /**
   * Relying solely on the OpenTelemetry instrumentation to correctly notify this SpanProcessor when
   * a span has ended opens up the possibility of a memory leak in the event a bug is encountered
   * within the instrumentation layer that prevents a span from being ended.
   *
   * <p>Will follow up with a more robust solution to this potential problem.
   */
  @Override
  public void onEnd(ReadableSpan span) {
    if (isEntry(span)) {
      registry.unregister(span.getSpanContext());
    }
  }

  private boolean isEntry(ReadableSpan span) {
    return span.getKind() == SpanKind.SERVER || span.getKind() == SpanKind.CONSUMER;
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
