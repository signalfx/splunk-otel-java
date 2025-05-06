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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimpleTraceRegistryTest {
  private final SimpleTraceRegistry registry = new SimpleTraceRegistry();

  @Test
  void registerTrace() {
    var spanContext = Snapshotting.spanContext().build();
    registry.register(spanContext);
    assertTrue(registry.isRegistered(spanContext));
  }

  @Test
  void unregisteredTracesAreNotRegisteredForProfiling() {
    var spanContext = Snapshotting.spanContext().build();
    assertFalse(registry.isRegistered(spanContext));
  }

  @Test
  void unregisterTraceForProfiling() {
    var spanContext = Snapshotting.spanContext().build();

    registry.register(spanContext);
    registry.unregister(spanContext);

    assertFalse(registry.isRegistered(spanContext));
  }
}
