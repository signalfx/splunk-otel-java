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

public class ThreadDumpRegion {
  public final String threadDump;
  public int startIndex;
  public int endIndex;

  public ThreadDumpRegion(String threadDump) {
    this(threadDump, 0, threadDump.length());
  }

  public ThreadDumpRegion(String threadDump, int startIndex, int endIndex) {
    this.threadDump = threadDump;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
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
    return threadDump.substring(startIndex, endIndex);
  }
}
