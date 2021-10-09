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
import static com.splunk.opentelemetry.profiler.context.StackToSpanLinkage.withoutLinkage;

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
    String spanId = event.getString("spanId");
    String traceId = event.getString("traceId");
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
      SpanLinkage linkage = new SpanLinkage(traceId, spanId, javaThreadId);
      threadSpans.put(javaThreadId, linkage);
    }
  }

  /** Parses thread info from the raw stack string and links it to a span (if available). */
  public StackToSpanLinkage link(Instant time, String rawStack, RecordedEvent event) {
    String eventName = event.getEventType().getName();

    // Many GC and other VM threads don't actually have a stack...
    if (isStacklessThread(rawStack)) {
      return withoutLinkage(time, rawStack, eventName);
    }
    long threadId = descriptorParser.parseThreadId(rawStack);
    if (threadId == CANT_PARSE_THREAD_ID) {
      return withoutLinkage(time, rawStack, eventName);
    }
    return link(time, rawStack, threadId, event);
  }

  private boolean isStacklessThread(String rawStack) {
    int firstNewline = rawStack.indexOf('\n');
    return (firstNewline == -1)
        || (firstNewline == rawStack.length() - 1)
        || (rawStack.indexOf('\n', firstNewline + 1) == -1);
  }

  /** Links the raw stack with the span info for the given thread. */
  StackToSpanLinkage link(Instant time, String rawStack, long threadId, RecordedEvent event) {
    String eventName = event.getEventType().getName();
    SpanLinkage spanLinkage = threadSpans.get(threadId);
    if (spanLinkage == null) {
      // We don't have an active span for this stack
      return withoutLinkage(time, rawStack, eventName);
    }

    return new StackToSpanLinkage(time, rawStack, eventName, spanLinkage);
  }

  // Exists for testing
  int inFlightThreadCount() {
    return threadSpans.size();
  }
}
