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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Supplier;

/**
 * Custom {@link SpanProcessor} implementation that will 1. register traces for snapshot profiling
 * and 2. activate profiling for the thread processing the trace. <br>
 * <br>
 * <b>Implementation Note</b><br>
 * The current snapshot profiling extension supports profiling one thread per trace at a time. If
 * the trace spans multiple threads -- whether because a service is invoked multiple time
 * concurrently by an upstream caller or work is delegated to background threads -- only a single
 * thread associated with that trace (specifically the thread associated with the "Entry" span) will
 * be profiled at a time.
 */
public class SnapshotProfilingSpanProcessor implements SpanProcessor {
  private final TraceRegistry registry;
  private final Supplier<StackTraceSampler> sampler;

  SnapshotProfilingSpanProcessor(TraceRegistry registry, Supplier<StackTraceSampler> sampler) {
    this.registry = registry;
    this.sampler = sampler;
  }

  @Override
  public void onStart(Context context, ReadWriteSpan span) {
    if (isEntry(span)) {
      Volume volume = Volume.from(context);
      if (volume == Volume.HIGHEST) {
        registry.register(span.getSpanContext());
      }
    }

    if (isEntry(span) && registry.isRegistered(span.getSpanContext())) {
      sampler.get().start(span.getSpanContext());
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
      sampler.get().stop(span.getSpanContext());
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
