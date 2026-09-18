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

import static com.splunk.opentelemetry.profiler.threaddump.StackTraceParserTest.readDumpFromResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DeadlockDataExtractorTest {
  @Test
  void extractsOwnableSynchronizerOwners() {
    Map<String, String> lockOwners =
        DeadlockDataExtractor.extractLockOwners(readDumpFromResource("thread-dump3.txt"));

    assertThat(lockOwners)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "java.util.concurrent.locks.ReentrantLock$NonfairSync@b937b3e60",
                "TEST-DEADLOCK-1-B",
                "java.util.concurrent.locks.ReentrantLock$NonfairSync@b937b3e30",
                "TEST-DEADLOCK-1-A",
                "java.util.concurrent.locks.ReentrantLock$NonfairSync@b9215d890",
                "TEST-DEADLOCK-4-B",
                "java.util.concurrent.locks.ReentrantLock$NonfairSync@b9215d860",
                "TEST-DEADLOCK-4-A"));
  }

  @Test
  void returnsEmptyMapWhenNoDeadlocksArePresent() {
    assertThat(DeadlockDataExtractor.extractLockOwners("No deadlocks found.")).isEmpty();
  }
}
