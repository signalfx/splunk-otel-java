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

import static com.splunk.opentelemetry.profiler.context.StackWallHelper.indexOfWithinStack;

/**
 * Class that parses the "descriptor" line from a stack trace. At the moment, it only parses out the
 * thread ID.
 */
class StackDescriptorLineParser {

  static final long CANT_PARSE_THREAD_ID = Long.MIN_VALUE;

  /**
   * The first line is a meta/descriptor that has information about the following call stack. This
   * method parses out the thread id, which is the second field (space separated).
   */
  long parseThreadId(String wallOfStacks, int descriptorLineIndex, int stackEndIndex) {
    // Require a quoted thread name field at the start
    if (descriptorLineIndex >= stackEndIndex || wallOfStacks.charAt(descriptorLineIndex) != '"') {
      return CANT_PARSE_THREAD_ID;
    }
    int secondQuote = indexOfWithinStack(wallOfStacks, '"', descriptorLineIndex + 1, stackEndIndex);
    if (secondQuote == -1) {
      return CANT_PARSE_THREAD_ID;
    }
    int firstSpaceAfterSecondQuote = secondQuote + 1;
    if (firstSpaceAfterSecondQuote >= stackEndIndex - 2
        || wallOfStacks.charAt(firstSpaceAfterSecondQuote) != ' ') {
      return CANT_PARSE_THREAD_ID;
    }
    if (wallOfStacks.charAt(firstSpaceAfterSecondQuote + 1) != '#') {
      // Unexpected format, fail to parse
      return CANT_PARSE_THREAD_ID;
    }
    int secondSpace =
        indexOfWithinStack(wallOfStacks, ' ', firstSpaceAfterSecondQuote + 1, stackEndIndex);
    if (secondSpace == -1) {
      return CANT_PARSE_THREAD_ID;
    }
    try {
      return Long.parseLong(wallOfStacks.substring(firstSpaceAfterSecondQuote + 2, secondSpace));
    } catch (NumberFormatException e) {
      return CANT_PARSE_THREAD_ID;
    }
  }
}
