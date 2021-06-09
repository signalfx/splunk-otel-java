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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class FilterSortedRecordingFileTest {

  @Test
  void testFiltersort() {
    Path path = Paths.get("/path/to/file.jfr");
    Instant now = Instant.now();
    RecordedEvent event1 = makeEvent("jdk.ThreadDump", now.plus(Duration.ofSeconds(1)));
    RecordedEvent event2 = makeEvent("jdk.DoNotCareX", now.plus(Duration.ofSeconds(2)));
    RecordedEvent event3 = makeEvent("otel.ContextAttached", now.plus(Duration.ofSeconds(3)));
    RecordedEvent event4 = makeEvent("jdk.ThreadDump", now.plus(Duration.ofSeconds(4)));
    RecordedEvent event5 = makeEvent("otel.ContextAttached", now.plus(Duration.ofSeconds(5)));
    Stream<RecordedEvent> str = Stream.of(event5, event1, event4, event3, event2);
    List<RecordedEvent> expected = Arrays.asList(event1, event3, event4, event5);

    RecordedEventStream delegate = mock(RecordedEventStream.class);
    when(delegate.open(path)).thenReturn(str);
    RecordedEventStream.Factory delegateFactory = () -> delegate;
    FilterSortedRecordingFile recordingFile = new FilterSortedRecordingFile(delegateFactory);
    List<RecordedEvent> result = recordingFile.open(path).collect(Collectors.toList());
    assertEquals(expected, result);
  }

  private RecordedEvent makeEvent(String name, Instant startTime) {
    RecordedEvent event = mock(RecordedEvent.class);
    EventType eventType = mock(EventType.class);
    when(event.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn(name);
    when(event.getStartTime()).thenReturn(startTime);
    return event;
  }
}
