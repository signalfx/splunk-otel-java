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
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventPeriodsTest {

  @Mock Function<String, String> configFinder;

  @Test
  void testNotCachedFirstParse() {
    EventPeriods eventPeriods = new EventPeriods(configFinder);
    when(configFinder.apply("jdk.SomeEvent#period")).thenReturn("250 ms");
    Duration result = eventPeriods.getDuration("jdk.SomeEvent");
    assertEquals(Duration.ofMillis(250), result);
  }

  @Test
  void testCached() {
    EventPeriods eventPeriods = new EventPeriods(configFinder);
    when(configFinder.apply("jdk.SomeEvent#period"))
        .thenReturn("26 s")
        .thenThrow(new IllegalStateException());
    Duration result1 = eventPeriods.getDuration("jdk.SomeEvent");
    Duration result2 = eventPeriods.getDuration("jdk.SomeEvent");
    Duration result3 = eventPeriods.getDuration("jdk.SomeEvent");
    assertEquals(Duration.ofSeconds(26), result1);
    assertEquals(Duration.ofSeconds(26), result2);
    assertEquals(Duration.ofSeconds(26), result3);
  }

  @Test
  void testNotFoundAlsoCached() {
    EventPeriods eventPeriods = new EventPeriods(configFinder);
    when(configFinder.apply("jdk.SomeEvent#period"))
        .thenReturn(null)
        .thenThrow(new IllegalStateException());
    Duration result1 = eventPeriods.getDuration("jdk.SomeEvent");
    Duration result2 = eventPeriods.getDuration("jdk.SomeEvent");
    assertEquals(EventPeriods.UNKNOWN, result1);
    assertEquals(EventPeriods.UNKNOWN, result2);
  }

  @Test
  void testNotParsedAlsoCached() {
    EventPeriods eventPeriods = new EventPeriods(configFinder);
    when(configFinder.apply("jdk.SomeEvent#period"))
        .thenReturn("BLEAK BLOOP")
        .thenThrow(new IllegalStateException());
    Duration result1 = eventPeriods.getDuration("jdk.SomeEvent");
    Duration result2 = eventPeriods.getDuration("jdk.SomeEvent");
    assertEquals(EventPeriods.UNKNOWN, result1);
    assertEquals(EventPeriods.UNKNOWN, result2);
  }

  @Test
  void testConfigNotFound() {
    EventPeriods eventPeriods = new EventPeriods(configFinder);
    when(configFinder.apply("jdk.SomeEvent#period")).thenReturn(null);
    Duration result = eventPeriods.getDuration("jdk.SomeEvent");
    assertEquals(EventPeriods.UNKNOWN, result);
  }

  @Test
  void testEveryChunk() {
    // Sometimes the JFR config might have the word "everyChunk" instead of an actual value
    EventPeriods eventPeriods = new EventPeriods(configFinder);
    when(configFinder.apply("jdk.SomeEvent#period")).thenReturn("everyChunk");
    Duration result = eventPeriods.getDuration("jdk.SomeEvent");
    assertEquals(EventPeriods.UNKNOWN, result);
  }
}
