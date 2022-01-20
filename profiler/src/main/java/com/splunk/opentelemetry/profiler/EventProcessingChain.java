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

package com.splunk.opentelemetry.profiler;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventProcessingChain {

  private static final Logger logger = LoggerFactory.getLogger(EventProcessingChain.class);

  private final SpanContextualizer spanContextualizer;
  private final ThreadDumpProcessor threadDumpProcessor;
  private final TLABProcessor tlabProcessor;
  private final PriorityQueue<RecordedEvent> buffer =
      new PriorityQueue<>(Comparator.comparing(RecordedEvent::getStartTime));
  private final EventStats eventStats =
      logger.isDebugEnabled() ? new EventStatsImpl() : new NoOpEventStats();
  private final ChunkTracker chunkTracker = new ChunkTracker();

  EventProcessingChain(
      SpanContextualizer spanContextualizer,
      ThreadDumpProcessor threadDumpProcessor,
      TLABProcessor tlabProcessor) {
    this.spanContextualizer = spanContextualizer;
    this.threadDumpProcessor = threadDumpProcessor;
    this.tlabProcessor = tlabProcessor;
  }

  void accept(RecordedEvent event) {
    String eventName = event.getEventType().getName();
    eventStats.incEventCount();
    switch (eventName) {
      case ContextAttached.EVENT_NAME:
        try (EventTimer eventTimer = eventStats.time(eventName)) {
          spanContextualizer.updateContext(event);
        }
        break;
      case ThreadDumpProcessor.EVENT_NAME:
        // TODO: is that correct?
        if (chunkTracker.threadDump(event)) {
          flushBuffer();
        }
        buffer.add(event);
        break;
      case TLABProcessor.NEW_TLAB_EVENT_NAME:
      case TLABProcessor.OUTSIDE_TLAB_EVENT_NAME:
        try (EventTimer eventTimer = eventStats.time(eventName)) {
          tlabProcessor.accept(event);
        }
        break;
    }
  }

  /**
   * Tells the processing chain that a work unit (JFR file) is complete and it can process what's in
   * the buffer. After flushing, the buffer will be empty.
   */
  public void flushBuffer() {
    buffer.forEach(dispatchContextualizedThreadDumps());
    buffer.clear();
    chunkTracker.reset();
  }

  private Consumer<RecordedEvent> dispatchContextualizedThreadDumps() {
    return event -> {
      String eventName = event.getEventType().getName();
      switch (eventName) {
        case ContextAttached.EVENT_NAME:
          try (EventTimer eventTimer = eventStats.time(eventName)) {
            spanContextualizer.updateContext(event);
          }
          break;
        case ThreadDumpProcessor.EVENT_NAME:
          try (EventTimer eventTimer = eventStats.time(eventName)) {
            threadDumpProcessor.accept(event);
          }
          break;
      }
    };
  }

  public void logEventStats() {
    eventStats.logEventStats();
  }

  private interface EventStats {
    void incEventCount();

    EventTimer time(String name);

    void logEventStats();
  }

  private static class NoOpEventStats implements EventStats {

    @Override
    public void incEventCount() {}

    @Override
    public EventTimer time(String name) {
      return null;
    }

    @Override
    public void logEventStats() {}
  }

  private static class EventStatsImpl implements EventStats {
    long eventCount;
    Map<String, EventCounter> eventCounters = new HashMap<>();

    @Override
    public void incEventCount() {
      eventCount++;
    }

    @Override
    public EventTimer time(String name) {
      EventCounter counter = eventCounters.computeIfAbsent(name, (k) -> new EventCounter());
      counter.count++;
      return new EventTimer(counter);
    }

    private void reset() {
      eventCount = 0;
      eventCounters.clear();
    }

    @Override
    public void logEventStats() {
      List<Map.Entry<String, EventCounter>> events = new ArrayList<>(eventCounters.entrySet());
      events.sort(Comparator.comparingLong(entry -> -entry.getValue().timeSpent));
      long totalTime = 0;
      for (Map.Entry<String, EventCounter> entry : events) {
        logger.debug(
            "Handled {} {} events in {}ms",
            entry.getValue().count,
            entry.getKey(),
            TimeUnit.NANOSECONDS.toMillis(entry.getValue().timeSpent));
        totalTime += entry.getValue().timeSpent;
      }
      logger.debug(
          "In total handled {} events in {}ms",
          eventCount,
          TimeUnit.NANOSECONDS.toMillis(totalTime));
      reset();
    }
  }

  private static class EventCounter {
    long count;
    long timeSpent;
  }

  private static class EventTimer implements AutoCloseable {
    private final long start;
    private final EventCounter counter;

    EventTimer(EventCounter counter) {
      this.counter = counter;
      this.start = System.nanoTime();
    }

    @Override
    public void close() {
      long end = System.nanoTime();
      counter.timeSpent += end - start;
    }
  }

  /**
   * Helper class for detecting when parsing advances to next chunk. We'll sort events only for the
   * current chunk, similarly how jfr command line tool does when printing events. While jfr command
   * line tool uses an internal method to detect last event of chunk we do this in a bit roundabout
   * way. As event types are recreated for each chunk we know that we are in a new chunk when event
   * type of context attach or thread dump event doesn't match what we have recorded. When chunk
   * change is detected process buffered events and clear the buffer.
   */
  private static class ChunkTracker {
    private EventType contextAttachedEventType = null;
    private EventType threadDumpEventType = null;

    /** @return true when event belongs to a new chunk */
    boolean contextAttached(RecordedEvent event) {
      EventType currentEventType = event.getEventType();
      if (contextAttachedEventType == null) {
        contextAttachedEventType = currentEventType;
      }
      // each chunk is parsed by a new parser that recreates event type
      return contextAttachedEventType != currentEventType;
    }

    /** @return true when event belongs to a new chunk */
    boolean threadDump(RecordedEvent event) {
      EventType currentEventType = event.getEventType();
      if (threadDumpEventType == null) {
        threadDumpEventType = currentEventType;
      }
      // each chunk is parsed by a new parser that recreates event type
      return threadDumpEventType != currentEventType;
    }

    void reset() {
      contextAttachedEventType = null;
      threadDumpEventType = null;
    }
  }
}
