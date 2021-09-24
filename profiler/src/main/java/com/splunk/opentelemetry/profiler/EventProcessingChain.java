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
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

class EventProcessingChain {

  private final SpanContextualizer spanContextualizer;
  private final ThreadDumpProcessor threadDumpProcessor;
  private final TLABProcessor tlabProcessor;
  private final PriorityQueue<RecordedEvent> buffer =
      new PriorityQueue<>(Comparator.comparing(RecordedEvent::getStartTime));

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
    switch (eventName) {
      case ContextAttached.EVENT_NAME:
      case ThreadDumpProcessor.EVENT_NAME:
        buffer.add(event);
        break;
      case TLABProcessor.NEW_TLAB_EVENT_NAME:
      case TLABProcessor.OUTSIDE_TLAB_EVENT_NAME:
        tlabProcessor.accept(event);
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
  }

  private Consumer<RecordedEvent> dispatchContextualizedThreadDumps() {
    return event -> {
      String eventName = event.getEventType().getName();
      switch (eventName) {
        case ContextAttached.EVENT_NAME:
          spanContextualizer.updateContext(event);
          break;
        case ThreadDumpProcessor.EVENT_NAME:
          threadDumpProcessor.accept(event);
          break;
      }
    };
  }
}
