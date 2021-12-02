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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.common.Attributes;
import java.time.Duration;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class LogDataCommonAttributesTest {

  @Test
  void testBuild() {
    String sourceType = "otel.profiling";
    String eventName = "core.core.unicorn";
    Attributes expected =
        Attributes.builder()
            .put(SOURCE_TYPE, sourceType)
            .put(SOURCE_EVENT_NAME, eventName)
            .put(SOURCE_EVENT_PERIOD, 999L)
            .build();

    EventPeriods periods = mock(EventPeriods.class);
    RecordedEvent event = mock(RecordedEvent.class);
    EventType eventType = mock(EventType.class);

    when(event.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn(eventName);
    when(periods.getDuration(eventName)).thenReturn(Duration.ofMillis(999));

    LogDataCommonAttributes logDataAttributes = new LogDataCommonAttributes(periods);

    Attributes result = logDataAttributes.build(event.getEventType().getName());
    assertEquals(expected, result);
  }
}
