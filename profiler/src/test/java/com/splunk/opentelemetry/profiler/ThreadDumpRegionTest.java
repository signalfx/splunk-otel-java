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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class ThreadDumpRegionTest {
  @Test
  void testAllStacksFound() {
    ThreadDumpRegion region = new ThreadDumpRegion(readDumpFromResource("thread-dump1.txt"), 0, 0);
    List<String> result = new ArrayList<>();

    while (region.findNextStack()) {
      result.add(region.getCurrentRegion());
    }

    assertEquals(77, result.size());
  }

  @Test
  void skipClustersWithoutDoubleQuote() {
    ThreadDumpRegion region = new ThreadDumpRegion("something\n\n\"else\"", 0, 0);
    assertTrue(region.findNextStack());
    assertEquals(region.getCurrentRegion(), "\"else\"");
    assertFalse(region.findNextStack());
  }

  @Test
  void edgeCase1_simplyHitsEnd() {
    ThreadDumpRegion region = new ThreadDumpRegion("\"something\"\n\n", 0, 0);
    assertTrue(region.findNextStack());
    assertEquals(region.getCurrentRegion(), "\"something\"");
    assertFalse(region.findNextStack());
  }

  @Test
  void edgeCase2_emptyString() {
    ThreadDumpRegion region = new ThreadDumpRegion("", 0, 0);
    assertFalse(region.findNextStack());
  }

  @Test
  void testFindsMatchInRange() {
    assertEquals(3, new ThreadDumpRegion("abcdef", 0, 4).indexOf('d', 2));
  }

  @Test
  void testDoesNotFindBeforeRange() {
    assertEquals(-1, new ThreadDumpRegion("abcdef", 0, 4).indexOf('b', 2));
  }

  @Test
  void testDoesNotFindAfterRange() {
    assertEquals(-1, new ThreadDumpRegion("abcdef", 0, 4).indexOf('e', 2));
  }

  static String readDumpFromResource(String resourcePath) {
    try (InputStream in =
        ThreadDumpRegionTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
      return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
