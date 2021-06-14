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

package com.splunk.opentelemetry.profiler.context;

import static com.splunk.opentelemetry.profiler.context.StackDescriptorLineParser.CANT_PARSE_THREAD_ID;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StackDescriptorLineParserTest {

  static final String GOOD_DESC =
      "\"AwesomeSpanHere\" #31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000 nid=0x3703 in Object.wait()  [0x000070000c7d9000]\n";
  static final String NO_QUOTES_DESC =
      "MissingQuotesHere #31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000 nid=0x3703 in Object.wait()  [0x000070000c7d9000]\n";

  @Test
  void testHappyPathParses() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId(GOOD_DESC);
    assertEquals(31, result);
  }

  @Test
  void testHappyPathSimplerInput() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId("\"test\" #33 ");
    assertEquals(33, result);
  }

  @Test
  void testMissingQuotes() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId(NO_QUOTES_DESC);
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testPurposefullyDeviantInput() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId(".#31 daemon x");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testQuoteAtEnd() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result =
        parser.parseThreadId(
            ".#31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000\"");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testEntireLineQuoted() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result =
        parser.parseThreadId(
            "\".#31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000\"");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testFallOffTheEnd() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result =
        parser.parseThreadId(
            "\".#31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000\" ");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testFallOffTheEndWithHash() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId("\"test\" #");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testSecondSpaceMissing() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId("\"test\" #33");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testNotNumeric() {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId("\"test\" #zoinks ");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }
}
