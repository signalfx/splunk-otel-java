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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThreadDumpToStacksTest {

  String threadDumpResult;

  @BeforeEach
  void setup() throws Exception {
    InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("thread-dump1.txt");
    threadDumpResult = new String(in.readAllBytes(), StandardCharsets.UTF_8);
  }

  @Test
  void testStream() {
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks();
    ThreadDumpRegion region = new ThreadDumpRegion(threadDumpResult, 0, 0);
    List<String> result = new ArrayList<>();

    while (threadDumpToStacks.findNext(region)) {
      result.add(region.toString());
    }

    assertEquals(77, result.size());
  }

  @Test
  void skipClustersWithoutDoubleQuote() {
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks();

    ThreadDumpRegion region = new ThreadDumpRegion("something\n\n\"else\"", 0, 0);
    assertTrue(threadDumpToStacks.findNext(region));
    assertEquals(region.toString(), "\"else\"");
    assertFalse(threadDumpToStacks.findNext(region));
  }

  @Test
  void edgeCase1_simplyHitsEnd() {
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks();

    ThreadDumpRegion region = new ThreadDumpRegion("\"something\"\n\n", 0, 0);
    assertTrue(threadDumpToStacks.findNext(region));
    assertEquals(region.toString(), "\"something\"");
    assertFalse(threadDumpToStacks.findNext(region));
  }

  @Test
  void edgeCase2_emptyString() {
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks();
    ThreadDumpRegion region = new ThreadDumpRegion("", 0, 0);
    assertFalse(threadDumpToStacks.findNext(region));
  }
}
