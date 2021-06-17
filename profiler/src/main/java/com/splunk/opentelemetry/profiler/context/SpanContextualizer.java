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

import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of span scope changes and, when applicable, can wrap the RecordedEvent with span
 * context information. This class is not thread safe.
 */
public class SpanContextualizer {

  private static final Logger logger = LoggerFactory.getLogger(SpanContextualizer.class);

  private final Pattern lineSplitter = Pattern.compile("\n");
  private final ThreadContextTracker threadContextTracker = new ThreadContextTracker();
  private final StackDescriptorLineParser descriptorParser = new StackDescriptorLineParser();

  /**
   * This updates the tracked thread context for a given span. This must only be called with
   * ContextAttached events.
   */
  public void updateContext(RecordedEvent event) {
    if (event.getByte("direction") == ContextAttached.IN) {
      threadContextStarting(event);
    } else {
      threadContextEnding(event);
    }
  }

  /** Parses thread info from the raw stack string and links it to a span (if available). */
  public StackToSpanLinkage link(Instant time, String rawStack, RecordedEvent event) {
    List<String> frames = Arrays.asList(lineSplitter.split(rawStack));
    if (frames.size() < 2) {
      // Many GC and other VM threads don't actually have a stack...
      return withoutLinkage(time, rawStack, event);
    }
    long threadId = descriptorParser.parseThreadId(frames.get(0));
    if (threadId == CANT_PARSE_THREAD_ID) {
      return withoutLinkage(time, rawStack, event);
    }
    return link(time, rawStack, threadId, event);
  }

  /** Links the raw stack with the span info for the given thread. */
  StackToSpanLinkage link(Instant time, String rawStack, long threadId, RecordedEvent event) {
    List<SpanLinkage> inFlightSpansForThisThread =
        threadContextTracker.getInFlightSpansForThread(threadId);

    if (inFlightSpansForThisThread.isEmpty()) {
      // We don't know about any in-flight spans for this stack
      return withoutLinkage(time, rawStack, event);
    }

    // This thread has a span happening, augment with span details
    if ((inFlightSpansForThisThread.size() > 1) && logger.isDebugEnabled()) {
      logger.debug("!! Nested spans detected: We will only use the last span for now...");
      logger.debug(
          "traceIds for thread -> {}",
          inFlightSpansForThisThread.stream()
              .map(SpanLinkage::getTraceId)
              .collect(Collectors.joining(" ")));
      logger.debug(
          "spans for thread -> {}",
          inFlightSpansForThisThread.stream()
              .map(SpanLinkage::getSpanId)
              .collect(Collectors.joining(" ")));
    }
    SpanLinkage spanLinkage = inFlightSpansForThisThread.get(inFlightSpansForThisThread.size() - 1);
    logger.warn(
        "!!!!! LINKED IT UP!! thread={} span={} trace={}",
        threadId,
        spanLinkage.getSpanId(),
        spanLinkage.getTraceId());
    //    logger.warn(rawStack);
    return new StackToSpanLinkage(time, rawStack, event, spanLinkage);
  }

  private void threadContextStarting(RecordedEvent event) {
    String spanId = event.getString("spanId");
    String traceId = event.getString("traceId");
    long javaThreadId = event.getThread().getJavaThreadId();
    logger.debug(
        "SPAN THREAD CONTEXT START : [{}] {} {} at {}",
        javaThreadId,
        traceId,
        spanId,
        event.getStartTime());

    SpanLinkage linkage = new SpanLinkage(traceId, spanId, javaThreadId);

    threadContextTracker.addLinkage(linkage);
  }

  private void threadContextEnding(RecordedEvent event) {
    String spanId = event.getString("spanId");
    String traceId = event.getString("traceId");
    long threadId = event.getThread().getJavaThreadId();
    logger.debug(
        "SPAN THREAD CONTEXT END : [{}] {} {} at {}",
        threadId,
        traceId,
        spanId,
        event.getStartTime());

    threadContextTracker.unlink(traceId, spanId, threadId);
  }

  // Exists for testing
  int inFlightThreadCount() {
    return threadContextTracker.getNumberOfInFlightThreads();
  }

  // Exists for testing
  int inFlightSpanCount() {
    return threadContextTracker.getNumberOfInFlightSpans();
  }
}
