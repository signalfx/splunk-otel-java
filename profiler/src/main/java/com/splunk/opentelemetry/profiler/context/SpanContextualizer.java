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

import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
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
   * This updates the tracked context for a This must only be called with ContextAttached events.
   */
  public void updateContext(RecordedEvent event) {
    if (event.getByte("direction") == ContextAttached.IN) {
      threadContextStarting(event);
    } else {
      threadContextEnding(event);
    }
  }

  /** Parses thread info from the raw stack string and links it to a span (if available). */
  public StackToSpanLinkage link(String rawStack) {
    List<String> frames = Arrays.asList(lineSplitter.split(rawStack));
    if (frames.size() < 2) {
      // Many GC and other VM threads don't actually have a stack...
      return StackToSpanLinkage.withoutLinkage(rawStack);
    }
    long threadId = descriptorParser.parseThreadId(frames.get(0));
    if (threadId == CANT_PARSE_THREAD_ID) {
      return StackToSpanLinkage.withoutLinkage(rawStack);
    }
    return link(rawStack, threadId);
  }

  /** Links the raw stack with the span info for the given thread. */
  StackToSpanLinkage link(String rawStack, long threadId) {
    List<SpanLinkage> inFlightSpansForThisThread =
        threadContextTracker.getInFlightSpansForThread(threadId);

    if (inFlightSpansForThisThread.isEmpty()) {
      // We don't know about any in-flight spans for this stack
      return StackToSpanLinkage.withoutLinkage(rawStack);
    }

    // This thread has a span happening, augment with span details
    if (inFlightSpansForThisThread.size() > 1) {
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
    return new StackToSpanLinkage(rawStack, spanLinkage);
  }

  private void threadContextStarting(RecordedEvent event) {
    String spanId = event.getString("spanId");
    String traceId = event.getString("traceId");
    RecordedThread thread = event.getThread();
    long threadId = thread.getJavaThreadId();
    logger.debug(
        "SPAN THREAD CONTEXT START : [{}] {} {} at {}",
        threadId,
        traceId,
        spanId,
        event.getStartTime());

    SpanLinkage linkage = new SpanLinkage(spanId, traceId, thread);

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
