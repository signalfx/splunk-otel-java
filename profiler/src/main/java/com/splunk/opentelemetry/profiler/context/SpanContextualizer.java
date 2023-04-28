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

package com.splunk.opentelemetry.profiler.context;

import static com.splunk.opentelemetry.profiler.context.StackDescriptorLineParser.CANT_PARSE_THREAD_ID;
import static java.util.logging.Level.FINE;

import com.splunk.opentelemetry.profiler.EventReader;
import com.splunk.opentelemetry.profiler.ThreadDumpRegion;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;

/**
 * Keeps track of span scope changes and, when applicable, can wrap the RecordedEvent with span
 * context information. This class is not thread safe.
 */
public class SpanContextualizer {

  private static final Logger logger = Logger.getLogger(SpanContextualizer.class.getName());

  private final Map<Long, SpanLinkage> threadSpans = new HashMap<>();
  private final StackDescriptorLineParser descriptorParser = new StackDescriptorLineParser();

  private final EventReader eventReader;

  public SpanContextualizer(EventReader eventReader) {
    this.eventReader = eventReader;
  }

  /**
   * This updates the tracked thread context for a given span. This must only be called with
   * ContextAttached events.
   */
  public void updateContext(IItem event) {
    // jdk 17 doesn't report thread for events that happened on a thread that has terminated by now
    IMCThread eventThread = eventReader.getThread(event);
    if (eventThread == null) {
      return;
    }
    String traceId = eventReader.getTraceId(event);
    String spanId = eventReader.getSpanId(event);
    long javaThreadId = eventThread.getThreadId();

    if (logger.isLoggable(FINE)) {
      logger.log(
          FINE,
          "Set thread context: [{0}] {1} {2} at {3}",
          new Object[] {javaThreadId, traceId, spanId, eventReader.getStartInstant(event)});
    }

    if (traceId == null || spanId == null) {
      threadSpans.remove(javaThreadId);
    } else {
      TraceFlags traceFlags = TraceFlags.fromByte(eventReader.getTraceFlags(event));
      SpanContext spanContext =
          SpanContext.create(traceId, spanId, traceFlags, TraceState.getDefault());
      SpanLinkage linkage = new SpanLinkage(spanContext, javaThreadId);
      threadSpans.put(javaThreadId, linkage);
    }
  }

  /**
   * Parses the thread info from the specified range of the wall of stacks, and returns the linkage
   * info for the thread referenced by that stack.
   */
  public SpanLinkage link(ThreadDumpRegion stack) {
    // Many GC and other VM threads don't actually have a stack...
    if (isStacklessThread(stack)) {
      return SpanLinkage.NONE;
    }
    long threadId = descriptorParser.parseThreadId(stack);
    if (threadId == CANT_PARSE_THREAD_ID) {
      return SpanLinkage.NONE;
    }
    return link(threadId);
  }

  public SpanLinkage link(long threadId) {
    return threadSpans.getOrDefault(threadId, SpanLinkage.NONE);
  }

  private boolean isStacklessThread(ThreadDumpRegion stack) {
    int firstNewline = stack.indexOf('\n', stack.startIndex);
    return (firstNewline == -1)
        || (firstNewline == stack.endIndex - 1)
        || (stack.indexOf('\n', firstNewline + 1) == -1);
  }

  // Exists for testing
  int inFlightThreadCount() {
    return threadSpans.size();
  }
}
