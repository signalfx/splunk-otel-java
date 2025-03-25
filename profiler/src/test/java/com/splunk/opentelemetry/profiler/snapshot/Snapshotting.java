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

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.sdk.trace.IdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

class Snapshotting {
  private static final Random RANDOM = new Random();
  private static final IdGenerator ID_GENERATOR = IdGenerator.random();

  static SnapshotProfilingSdkCustomizerBuilder customizer() {
    return new SnapshotProfilingSdkCustomizerBuilder();
  }

  static StackTraceBuilder stackTrace() {
    var threadId = RANDOM.nextLong(10_000);
    return new StackTraceBuilder()
        .with(Instant.now())
        .with(Duration.ofMillis(20))
        .withTraceId(randomTraceId())
        .withId(threadId)
        .withName("thread-" + threadId)
        .with(Thread.State.WAITING)
        .with(new RuntimeException());
  }

  static String randomTraceId() {
    return ID_GENERATOR.generateTraceId();
  }

  private Snapshotting() {}
}
