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
import jdk.jfr.consumer.RecordedEvent;

class EventProcessingChain {

  private final SpanContextualizer spanContextualizer;
  private final ThreadDumpProcessor threadDumpProcessor;

  EventProcessingChain(
      SpanContextualizer spanContextualizer, ThreadDumpProcessor threadDumpProcessor) {
    this.spanContextualizer = spanContextualizer;
    this.threadDumpProcessor = threadDumpProcessor;
  }

  void accept(RecordedEvent event) {
    String eventName = event.getEventType().getName();
    switch (eventName) {
      case ContextAttached.EVENT_NAME:
        spanContextualizer.updateContext(event);
        break;
      case "jdk.ThreadDump":
        threadDumpProcessor.accept(event);
        break;
    }
  }
}
