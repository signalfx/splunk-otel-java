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

import static java.util.logging.Level.FINE;

import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.allocation.sampler.RateLimitingAllocationEventSampler;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.openjdk.jmc.common.item.IItem;

class EventProcessingChain {

  private static final Logger logger = Logger.getLogger(EventProcessingChain.class.getName());

  private final EventReader eventReader;
  private final SpanContextualizer spanContextualizer;
  private final ThreadDumpProcessor threadDumpProcessor;
  private final TLABProcessor tlabProcessor;
  private final List<IItem> buffer = new ArrayList<>();
  private final EventStats eventStats =
      logger.isLoggable(FINE) ? new EventStatsImpl() : new NoOpEventStats();

  EventProcessingChain(
      EventReader eventReader,
      SpanContextualizer spanContextualizer,
      ThreadDumpProcessor threadDumpProcessor,
      TLABProcessor tlabProcessor) {
    this.eventReader = eventReader;
    this.spanContextualizer = spanContextualizer;
    this.threadDumpProcessor = threadDumpProcessor;
    this.tlabProcessor = tlabProcessor;
  }

  void accept(IItem event) {
    eventStats.incEventCount();
    buffer.add(event);
  }

  /**
   * Tells the processing chain that a work unit (JFR file) is complete and it can process what's in
   * the buffer. After flushing, the buffer will be empty.
   */
  public void flushBuffer() {
    buffer.sort(Comparator.comparingLong(eventReader::getStartTime));
    updateAllocationSampler();

    buffer.forEach(this::dispatchEvent);
    buffer.clear();
    tlabProcessor.flush();
    threadDumpProcessor.flush();
  }

  private static boolean isTlabEvent(IItem event) {
    String eventName = event.getType().getIdentifier();
    return TLABProcessor.NEW_TLAB_EVENT_NAME.equals(eventName)
        || TLABProcessor.OUTSIDE_TLAB_EVENT_NAME.equals(eventName);
  }

  private void updateAllocationSampler() {
    AllocationEventSampler allocationEventSampler = tlabProcessor.getAllocationEventSampler();
    if (!(allocationEventSampler instanceof RateLimitingAllocationEventSampler)) {
      return;
    }
    RateLimitingAllocationEventSampler sampler =
        (RateLimitingAllocationEventSampler) allocationEventSampler;

    long tlabEventCount = buffer.stream().filter(EventProcessingChain::isTlabEvent).count();
    if (tlabEventCount > 0) {
      Instant firsEvent = eventReader.getStartInstant(buffer.get(0));
      Instant lastEvent = eventReader.getStartInstant(buffer.get(buffer.size() - 1));

      sampler.updateSampler(tlabEventCount, firsEvent, lastEvent);
    }
  }

  private void dispatchEvent(IItem event) {
    String eventName = event.getType().getIdentifier();
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
      case TLABProcessor.NEW_TLAB_EVENT_NAME:
      case TLABProcessor.OUTSIDE_TLAB_EVENT_NAME:
      case TLABProcessor.ALLOCATION_SAMPLE_EVENT_NAME:
        try (EventTimer eventTimer = eventStats.time(eventName)) {
          tlabProcessor.accept(event);
        }
        break;
    }
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
        if (logger.isLoggable(FINE)) {
          logger.log(
              FINE,
              "Handled {0} {1} events in {2}ms",
              new Object[] {
                entry.getValue().count,
                entry.getKey(),
                TimeUnit.NANOSECONDS.toMillis(entry.getValue().timeSpent)
              });
        }
        totalTime += entry.getValue().timeSpent;
      }
      if (logger.isLoggable(FINE)) {
        logger.log(
            FINE,
            "In total handled {0} events in {1}ms",
            new Object[] {eventCount, TimeUnit.NANOSECONDS.toMillis(totalTime)});
      }
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
}
