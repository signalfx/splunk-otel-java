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
 * This class turns the "wall of stacks" from the jdk.ThreadDump event into Stream<String>, where
 * each String in the Stream is a stack trace. It purposefully avoids String.split("\n\n") in order
 * to help reduce allocations, especially for filtered stacks.
 */
public class ThreadDumpToStacks {
  public boolean findNext(ThreadDumpRegion region) {
    while (findNextCandidate(region)) {
      if (region.threadDump.charAt(region.startIndex) == '"') {
        return true;
      }
    }

    return false;
  }

  private boolean findNextCandidate(ThreadDumpRegion region) {
    String threadDump = region.threadDump;
    int start = region.endIndex;

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
      end = region.threadDump.length();
    }

    region.startIndex = start;
    region.endIndex = end;
    return true;
  }
}
