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

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class turns the "wall of stacks" from the jdk.ThreadDump event
 * into Stream<String>, where each String in the Stream is a stack trace.
 * It purposefully avoids String.split("\n\n") in order to help reduce
 * allocations, especially for filtered stacks.
 */
public class ThreadDumpToStacks {

  private final AgentInternalsFilter agentInternalsFilter;

  public ThreadDumpToStacks(AgentInternalsFilter agentInternalsFilter) {
    this.agentInternalsFilter = agentInternalsFilter;
  }

  public Stream<String> toStream(String wallOfStacks) {
    Spliterator<String> spliterator = new StackSpliterator(wallOfStacks, agentInternalsFilter);
    return StreamSupport.stream(spliterator, false);
  }

  private static class StackSpliterator extends Spliterators.AbstractSpliterator<String> {
    private final String wallOfStacks;
    private final AgentInternalsFilter agentInternalsFilter;
    private int start = 0;
    private int next = 0;
    private boolean done = false;

    public StackSpliterator(String wallOfStacks, AgentInternalsFilter agentInternalsFilter) {
      super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED);
      this.wallOfStacks = wallOfStacks;
      this.agentInternalsFilter = agentInternalsFilter;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
      while (true) {
        if (done) {
          return false;
        }
        next = wallOfStacks.indexOf("\n\n", start);
        if (next == -1) {
          next = wallOfStacks.lastIndexOf('\n', start);
        }
        if (next == -1) {
          done = true;
          return false;
        }
        // Reached the end of the wall, so just set next to the end
        if (next < start) {
          next = wallOfStacks.length() - 1;
        }
        if (agentInternalsFilter.test(wallOfStacks, start, next)) {
          action.accept(advanceNextStack());
          return true;
        }
        start = next + 1;
        while ((start < wallOfStacks.length()) && (wallOfStacks.charAt(start) == '\n')) {
          start++;
        }
        if (start >= wallOfStacks.length()) {
          done = true;
          return false;
        }
      }
    }

    private String advanceNextStack() {
      String result = wallOfStacks.substring(start, next);
      if (next >= wallOfStacks.length() - 2) {
        done = true;
      }
      start = next + 2;
      next = start;
      return result;
    }
  }
}
