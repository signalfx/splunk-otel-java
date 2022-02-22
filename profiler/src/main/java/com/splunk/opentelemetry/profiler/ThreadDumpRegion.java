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

/**
 * Points to region within a thread dump, currently intended for accessing stack traces within it
 * without extracting them as separate strings.
 */
public class ThreadDumpRegion {
  public final String threadDump;
  public int startIndex;
  public int endIndex;

  public ThreadDumpRegion(String threadDump, int startIndex, int endIndex) {
    this.threadDump = threadDump;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  public String getCurrentRegion() {
    return threadDump.substring(startIndex, endIndex);
  }

  /**
   * Implementation of indexOf with result limited to this region. The expectation is that it is
   * only used to find something which should under normal circumstances always be present,
   * therefore the performance penalty of the underlying call to {@link String#indexOf(int, int)}
   * also checking further in the string (in case it was out of bounds) would be insignificant.
   */
  public int indexOf(int character, int fromIndex) {
    int result = threadDump.indexOf(character, fromIndex);
    if (result >= endIndex) {
      return -1;
    }
    return result;
  }

  /**
   * Find next stack trace in the thread dump this region is using. Modifies the region this
   * instance points to, to that stack trace. Returns false if no more stacks were found, in which
   * case the region it points to afterwards is undefined.
   */
  public boolean findNextStack() {
    while (findNextSection()) {
      if (threadDump.charAt(startIndex) == '"') {
        return true;
      }
    }

    return false;
  }

  private boolean findNextSection() {
    int start = endIndex;

    // skip over any newlines, returning failure in case we reach end of string this way
    while (true) {
      if (start >= threadDump.length()) {
        return false;
      } else if (threadDump.charAt(start) != '\n') {
        break;
      }
      start++;
    }

    int end = threadDump.indexOf("\n\n", start);
    if (end == -1) {
      end = threadDump.lastIndexOf('\n', start);
    }
    if (end == -1) {
      return false;
    }
    // Reached the end of the wall, so just set next to the end
    if (end < start) {
      end = threadDump.length();
    }

    startIndex = start;
    endIndex = end;
    return true;
  }
}
