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

import java.util.stream.Stream;

public class StackTraceFilter {

  static final String[] UNWANTED_PREFIXES =
      new String[] {
        "\"Batched Logs Exporter\"",
        "\"BatchSpanProcessor_WorkerThread-",
        "\"JFR Recorder Thread\"",
        "\"JFR Periodic Tasks\"",
        "\"JFR Recording Scheduler\"",
        "\"JFR Recording Sequencer\"",
        "\"Reference Handler\"",
        "\"Finalizer\"",
        "\"C1 CompilerThread",
        "\"Common-Cleaner\""
      };
  private final boolean includeAgentInternals;

  public StackTraceFilter(boolean includeAgentInternals) {
    this.includeAgentInternals = includeAgentInternals;
  }

  public boolean test(String wallOfStacks, int startIndex, int lastIndex) {
    if ((lastIndex == -1) || (lastIndex >= wallOfStacks.length())) {
      return false;
    }
    // Must start with a quote for the thread name
    if (wallOfStacks.charAt(startIndex) != '"') {
      return false;
    }
    // If the last newline before next is before the start, that means we have one line, so skip
    // that
    int previousNewlineIndex = wallOfStacks.lastIndexOf('\n', lastIndex - 1);
    if (previousNewlineIndex <= startIndex) {
      return false;
    }
    // two line cases
    if (wallOfStacks.lastIndexOf('\n', previousNewlineIndex - 1) <= startIndex) {
      return false;
    }
    if (includeAgentInternals) {
      return true;
    }
    if(Stream.of(StackTraceFilter.UNWANTED_PREFIXES)
            .anyMatch(prefix -> wallOfStacks.regionMatches(startIndex, prefix, 0, prefix.length()))){
      return false;
    }

    return !everyFrameIsJvmInternal(wallOfStacks, startIndex, lastIndex);
  }

  private boolean everyFrameIsJvmInternal(String wallOfStacks, int startIndex, int lastIndex) {
    int offsetToThreadState = wallOfStacks.indexOf('\n', startIndex) + 1;
    if (offsetToThreadState <= 0 || offsetToThreadState >= lastIndex) return false;
    int offsetToFirstFrame = wallOfStacks.indexOf('\n', offsetToThreadState) + 1;
    if (offsetToFirstFrame <= 0 || offsetToFirstFrame >= lastIndex) return false;
    return checkFramesInternal(wallOfStacks, offsetToFirstFrame, lastIndex);
  }

  private boolean checkFramesInternal(String wallOfStacks, int startOfFrame, int lastIndex) {
    if(startOfFrame == -1 || (startOfFrame >= lastIndex)){
      // reached the bottom, every item is jvm internal
      return true;
    }
    int nextLine = wallOfStacks.indexOf('\n', startOfFrame) + 1;
    if(wallOfStacks.regionMatches(startOfFrame, "\t-", 0, 2) ||
       wallOfStacks.regionMatches(startOfFrame, "\tat java.", 0, 9) ||
       wallOfStacks.regionMatches(startOfFrame, "\tat jdk.", 0, 8) ||
       wallOfStacks.regionMatches(startOfFrame, "\tat sun.", 0, 8)) {
      return checkFramesInternal(wallOfStacks, nextLine, lastIndex);
    }
    return false;
  }
}
