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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SNAPSHOT_PROFILING;

import com.splunk.opentelemetry.profiler.snapshot.registry.TraceRegistry;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Custom {@link SpanProcessor} implementation that will register traces for snapshot profiling<br>
 */
public class SnapshotProfilingSpanProcessor implements SpanProcessor {
  private final TraceRegistry registry;
  private final OrphanedTraceCleaner orphanedTraceCleaner;

  SnapshotProfilingSpanProcessor(TraceRegistry registry) {
    this.registry = registry;
    this.orphanedTraceCleaner = new OrphanedTraceCleaner(registry);
  }

  @Override
  public void onStart(Context context, ReadWriteSpan span) {
    if (isEntry(span)) {
      Volume volume = Volume.from(context);
      if (volume == Volume.HIGHEST) {
        registry.register(span.getSpanContext());
        orphanedTraceCleaner.register(span);
      }
    }

    if (isEntry(span) && registry.isRegistered(span.getSpanContext())) {
      span.setAttribute(SNAPSHOT_PROFILING, true);
    }
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
      orphanedTraceCleaner.unregister(span.getSpanContext());
    }
  }

  private boolean isEntry(ReadableSpan span) {
    return !span.getParentSpanContext().isValid() || span.getParentSpanContext().isRemote();
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  @Override
  public CompletableResultCode shutdown() {
    orphanedTraceCleaner.close();
    return SpanProcessor.super.shutdown();
  }
}
