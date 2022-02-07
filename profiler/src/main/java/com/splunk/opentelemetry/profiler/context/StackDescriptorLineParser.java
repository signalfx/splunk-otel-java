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

import com.splunk.opentelemetry.profiler.ThreadDumpRegion;

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
  long parseThreadId(ThreadDumpRegion stack) {
    String threadDump = stack.threadDump;
    // Require a quoted thread name field at the start
    if (stack.startIndex >= stack.endIndex || threadDump.charAt(stack.startIndex) != '"') {
      return CANT_PARSE_THREAD_ID;
    }
    int secondQuote = stack.indexOf('"', stack.startIndex + 1);
    if (secondQuote == -1) {
      return CANT_PARSE_THREAD_ID;
    }
    int firstSpaceAfterSecondQuote = secondQuote + 1;
    if (firstSpaceAfterSecondQuote >= stack.endIndex - 2
        || threadDump.charAt(firstSpaceAfterSecondQuote) != ' ') {
      return CANT_PARSE_THREAD_ID;
    }
    if (threadDump.charAt(firstSpaceAfterSecondQuote + 1) != '#') {
      // Unexpected format, fail to parse
      return CANT_PARSE_THREAD_ID;
    }
    int secondSpace = stack.indexOf(' ', firstSpaceAfterSecondQuote + 1);
    if (secondSpace == -1) {
      return CANT_PARSE_THREAD_ID;
    }
    try {
      return Long.parseLong(threadDump.substring(firstSpaceAfterSecondQuote + 2, secondSpace));
    } catch (NumberFormatException e) {
      return CANT_PARSE_THREAD_ID;
    }
  }
}
