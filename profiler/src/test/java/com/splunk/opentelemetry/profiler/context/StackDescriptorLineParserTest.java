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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class StackDescriptorLineParserTest {

  static final String GOOD_DESC =
      "\"AwesomeSpanHere\" #31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000 nid=0x3703 in Object.wait()  [0x000070000c7d9000]\n";
  static final String NO_QUOTES_DESC =
      "MissingQuotesHere #31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000 nid=0x3703 in Object.wait()  [0x000070000c7d9000]\n";

  /**
   * Prefixes or suffixes which could cause the result of parsing to change if they were erroneously
   * also processed. Used to test when processing a range within a wall of stacks to trigger
   * failures in case string regions out of the specified range are included in parsing.
   */
  static final String[] DISRUPTIVE_AFFIXES =
      new String[] {
        "", // space included so the simple case could be tested together with others in the same
        // loop
        "\"",
        "\" #77 ",
        "\"fake\" #77 ",
        "\"fake\" #none ",
        "\n",
        "\n\n"
      };

  @Test
  void testHappyPathParses() {
    long result = parseAndAssertWithAffixes(GOOD_DESC);
    assertEquals(31, result);
  }

  @Test
  void testHappyPathSimplerInput() {
    long result = parseAndAssertWithAffixes("\"test\" #33 ");
    assertEquals(33, result);
  }

  @Test
  void testMissingQuotes() {
    long result = parseAndAssertWithAffixes(NO_QUOTES_DESC);
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testPurposefullyDeviantInput() {
    long result = parseAndAssertWithAffixes(".#31 daemon x");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testQuoteAtEnd() {
    long result =
        parseAndAssertWithAffixes(
            ".#31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000\"");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testEntireLineQuoted() {
    long result =
        parseAndAssertWithAffixes(
            "\".#31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000\"");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testFallOffTheEnd() {
    long result =
        parseAndAssertWithAffixes(
            "\".#31 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000\" ");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testFallOffTheEndWithHash() {
    long result = parseAndAssertWithAffixes("\"test\" #");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testSecondSpaceMissing() {
    long result = parseAndAssertWithAffixes("\"test\" #33");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testNotNumeric() {
    long result = parseAndAssertWithAffixes("\"test\" #zoinks ");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  @Test
  void testEmptyString() {
    long result = parseAndAssertWithAffixes("");
    assertEquals(CANT_PARSE_THREAD_ID, result);
  }

  long parseAndAssertWithAffixes(String descriptorLine) {
    StackDescriptorLineParser parser = new StackDescriptorLineParser();
    long result = parser.parseThreadId(descriptorLine, 0, descriptorLine.length());

    Stream.of(DISRUPTIVE_AFFIXES)
        .forEach(
            it -> {
              assertEqualsWithAffixes(result, parser, descriptorLine, "", it);
              assertEqualsWithAffixes(result, parser, descriptorLine, it, "");
            });

    return result;
  }

  void assertEqualsWithAffixes(
      long baseResult,
      StackDescriptorLineParser parser,
      String descriptorLine,
      String prefix,
      String suffix) {
    String combined = prefix + descriptorLine + suffix;
    long result =
        parser.parseThreadId(combined, prefix.length(), prefix.length() + descriptorLine.length());
    assertEquals(baseResult, result, "stable with prefix " + prefix + ", suffix " + suffix);
  }
}
