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

package com.splunk.opentelemetry.profiler.threaddump;

import javax.annotation.Nullable;

/**
 * Points to region within a thread dump, currently intended for accessing stack traces within it
 * without extracting them as separate strings.
 */
public class ThreadDumpRegion {
  private final String threadDump;
  private final int startIndex;
  private final int endIndex;

  public ThreadDumpRegion(String threadDump, int startIndex, int endIndex) {
    this.threadDump = threadDump;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  public String getThreadDump() {
    return threadDump;
  }

  public int getStartIndex() {
    return startIndex;
  }

  public int getEndIndex() {
    return endIndex;
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

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("ThreadDumpRegion{");
    sb.append("region='").append(getCurrentRegion()).append('\'');
    sb.append(", startIndex=").append(startIndex);
    sb.append(", endIndex=").append(endIndex);
    sb.append('}');
    return sb.toString();
  }

  public static class Iterator {
    private final String threadDump;
    private int startIndex;
    private int endIndex;

    public Iterator(String threadDump) {
      this.threadDump = threadDump;
    }

    /**
     * Find next stack trace in the thread dump this region is using.
     *
     * @return next stack trace, or {@code null} if no more stack traces found
     */
    @Nullable
    public ThreadDumpRegion findNextStack() {
      while (findNextSection()) {
        if (threadDump.charAt(startIndex) == '"') {
          return new ThreadDumpRegion(threadDump, startIndex, endIndex);
        }
      }

      return null;
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
}
