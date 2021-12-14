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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.Configuration;
import com.splunk.opentelemetry.profiler.TLABProcessor;
import com.splunk.opentelemetry.profiler.ThreadDumpProcessor;
import io.opentelemetry.instrumentation.api.config.Config;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelevantEventsTest {

  RecordedEvent threadDump;
  RecordedEvent tlab;

  @BeforeEach
  void setup() {
    threadDump = mock(RecordedEvent.class);
    tlab = mock(RecordedEvent.class);
    EventType threadDumpType = type(ThreadDumpProcessor.EVENT_NAME);
    EventType tlabType = type(TLABProcessor.NEW_TLAB_EVENT_NAME);

    when(threadDump.getEventType()).thenReturn(threadDumpType);
    when(tlab.getEventType()).thenReturn(tlabType);
  }

  private EventType type(String name) {
    EventType type = mock(EventType.class);
    when(type.getName()).thenReturn(name);
    return type;
  }

  @Test
  void testTlabEnabled() {
    Config config = mock(Config.class);
    when(config.getBoolean(
            Configuration.CONFIG_KEY_TLAB_ENABLED, Configuration.DEFAULT_MEMORY_ENABLED))
        .thenReturn(true);
    RelevantEvents relevantEvents = RelevantEvents.create(config);
    assertTrue(relevantEvents.isRelevant(threadDump));
    assertTrue(relevantEvents.isRelevant(tlab));
  }

  @Test
  void testTlabNotEnabled() {
    Config config = mock(Config.class);
    when(config.getBoolean(
            Configuration.CONFIG_KEY_TLAB_ENABLED, Configuration.DEFAULT_MEMORY_ENABLED))
        .thenReturn(false);
    RelevantEvents relevantEvents = RelevantEvents.create(config);
    assertTrue(relevantEvents.isRelevant(threadDump));
    assertFalse(relevantEvents.isRelevant(tlab));
  }
}
