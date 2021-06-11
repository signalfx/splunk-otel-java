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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Keeps track of what threads are working on which spans when replaying an event stream. */
class ThreadContextTracker {

  private static final Logger logger = LoggerFactory.getLogger(ThreadContextTracker.class);

  private final Map<Long, Stack<SpanLinkage>> inFlightSpansByThreadId = new HashMap<>();
  private final Map<String, Stack<Long>> inFlightSpansToThreadId = new HashMap<>();

  List<SpanLinkage> getInfLightSpansForThread(long threadId) {
    Stack<SpanLinkage> result = inFlightSpansByThreadId.get(threadId);
    return result == null ? Collections.emptyList() : result;
  }

  void addLinkage(SpanLinkage linkage) {
    Stack<SpanLinkage> spanLinkages =
        inFlightSpansByThreadId.computeIfAbsent(linkage.getThreadId(), tid -> new Stack<>());
    spanLinkages.push(linkage);

    Stack<Long> threadIds =
        inFlightSpansToThreadId.computeIfAbsent(linkage.getSpanId(), sid -> new Stack<>());
    threadIds.push(linkage.getThreadId());
  }

  void unlink(String traceId, String spanId, long threadId) {
    Stack<Long> spanThreadStack = inFlightSpansToThreadId.get(spanId);
    if (spanThreadStack != null) {
      spanThreadStack.remove(threadId);
      if (spanThreadStack.isEmpty()) {
        inFlightSpansToThreadId.remove(spanId);
      }
    }
    removeFromInFlight(threadId, spanId, traceId);
  }

  private void removeFromInFlight(long threadId, String spanId, String traceId) {
    Stack<SpanLinkage> inFlightForThread = inFlightSpansByThreadId.get(threadId);
    if (inFlightForThread == null) {
      // No spans in flight for this thread....shouldn't happen
      logger.debug("!!! No spans in flight for thread {}", threadId);
      return;
    }
    SpanLinkage spanInfo = findLinkage(inFlightForThread, traceId, spanId);
    if (spanInfo == null || !inFlightForThread.remove(spanInfo)) {
      // We arrived in a bad state where we can't find our span info to remove from our tracked
      // "in-flight" spans.
      logger.debug("!!! Could not find our started span! trace = {} span = {}", traceId, spanId);
      logger.debug("!!! tried to find thread {} => {} {}", threadId, traceId, spanId);
    }
    if (inFlightForThread.isEmpty()) {
      inFlightSpansByThreadId.remove(threadId);
    }
  }

  // TODO: This is inefficient and we might want to have an additional structure/map for doing a
  // fast lookup rather than iterating
  private SpanLinkage findLinkage(
      List<SpanLinkage> inFlightForThread, String traceId, String spanId) {
    return inFlightForThread.stream()
        .filter(s -> s.matches(traceId, spanId))
        .findFirst()
        .orElse(null);
  }

  public int getNumberOfInFlightThreads() {
    return inFlightSpansByThreadId.size();
  }

  public int getNumberOfInFlightSpans() {
    return inFlightSpansToThreadId.size();
  }
}
