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

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of span scope changes and, when applicable, can wrap the RecordedEvent with span
 * context information. This class is not thread safe.
 */
public class SpanContextualizer {

  private static final Logger logger = LoggerFactory.getLogger(SpanContextualizer.class);

  private final Map<Long, SpanLinkage> threadSpans = new HashMap<>();
  private final StackDescriptorLineParser descriptorParser = new StackDescriptorLineParser();

  /**
   * This updates the tracked thread context for a given span. This must only be called with
   * ContextAttached events.
   */
  public void updateContext(RecordedEvent event) {
    String traceId = event.getString("traceId");
    String spanId = event.getString("spanId");
    long javaThreadId = event.getThread().getJavaThreadId();

    logger.debug(
        "Set thread context: [{}] {} {} at {}",
        javaThreadId,
        traceId,
        spanId,
        event.getStartTime());

    if (traceId == null || spanId == null) {
      threadSpans.remove(javaThreadId);
    } else {
      TraceFlags traceFlags = TraceFlags.fromByte(event.getByte("traceFlags"));
      SpanContext spanContext =
          SpanContext.create(traceId, spanId, traceFlags, TraceState.getDefault());
      SpanLinkage linkage = new SpanLinkage(spanContext, javaThreadId);
      threadSpans.put(javaThreadId, linkage);
    }
  }

  /** Parses thread info from the raw stack string and links it to a span (if available). */
  public StackToSpanLinkage link(Instant time, String rawStack, RecordedEvent event) {
    String eventName = event.getEventType().getName();
    SpanLinkage spanLinkage = link(rawStack, 0, rawStack.length());
    return new StackToSpanLinkage(time, rawStack, eventName, spanLinkage);
  }

  /**
   * Parses the thread info from the specified range of the wall of stacks, and returns the linkage
   * info for the thread referenced by that stack.
   */
  public SpanLinkage link(String wallOfStacks, int stackStartIndex, int stackEndIndex) {
    // Many GC and other VM threads don't actually have a stack...
    if (isStacklessThread(wallOfStacks, stackStartIndex, stackEndIndex)) {
      return SpanLinkage.NONE;
    }
    long threadId = descriptorParser.parseThreadId(wallOfStacks, stackStartIndex, stackEndIndex);
    if (threadId == CANT_PARSE_THREAD_ID) {
      return SpanLinkage.NONE;
    }
    return link(threadId);
  }

  public SpanLinkage link(long threadId) {
    return threadSpans.getOrDefault(threadId, SpanLinkage.NONE);
  }

  private boolean isStacklessThread(String wallOfStacks, int stackStartIndex, int stackEndIndex) {
    int firstNewline =
        StackWallHelper.indexOfWithinStack(wallOfStacks, '\n', stackStartIndex, stackEndIndex);
    return (firstNewline == -1)
        || (firstNewline == stackEndIndex - 1)
        || (StackWallHelper.indexOfWithinStack(wallOfStacks, '\n', firstNewline + 1, stackEndIndex)
            == -1);
  }

  // Exists for testing
  int inFlightThreadCount() {
    return threadSpans.size();
  }
}
