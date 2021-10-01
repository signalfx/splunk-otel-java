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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThreadDumpToStacksTest {

  String threadDumpResult;

  @BeforeEach
  void setup() throws Exception {
    InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("thread-dump1.txt");
    threadDumpResult = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
  }

  @Test
  void testStream() {
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks(new StackTraceFilter(false));
    Stream<String> resultStream = threadDumpToStacks.toStream(threadDumpResult);
    List<String> result = resultStream.collect(Collectors.toList());
    assertEquals(40, result.size());
    Stream.of(StackTraceFilter.UNWANTED_PREFIXES)
        .forEach(
            prefix -> {
              assertThat(result).noneMatch(stack -> stack.contains(prefix));
            });
  }
}
