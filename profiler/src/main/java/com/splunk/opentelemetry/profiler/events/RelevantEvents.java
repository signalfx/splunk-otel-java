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

package com.splunk.opentelemetry.profiler.events;

import com.splunk.opentelemetry.profiler.Configuration;
import com.splunk.opentelemetry.profiler.TLABProcessor;
import com.splunk.opentelemetry.profiler.ThreadDumpProcessor;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jdk.jfr.consumer.RecordedEvent;

public class RelevantEvents {

  private final Set<String> eventNames;

  private RelevantEvents(Set<String> eventNames) {
    this.eventNames = eventNames;
  }

  public static RelevantEvents create(ConfigProperties config) {
    Set<String> eventNames =
        new HashSet<>(Arrays.asList(ThreadDumpProcessor.EVENT_NAME, ContextAttached.EVENT_NAME));
    if (Configuration.getTLABEnabled(config)) {
      eventNames.add(TLABProcessor.NEW_TLAB_EVENT_NAME);
      eventNames.add(TLABProcessor.OUTSIDE_TLAB_EVENT_NAME);
    }
    return new RelevantEvents(eventNames);
  }

  public boolean isRelevant(RecordedEvent event) {
    return eventNames.contains(event.getEventType().getName());
  }
}
