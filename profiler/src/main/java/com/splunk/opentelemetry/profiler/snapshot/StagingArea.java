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

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;

/**
 * Acts as a location to stockpile gathered {@link StackTrace}s segmented by thread ID for bulk
 * exportation at some later point in time.
 */
interface StagingArea extends Closeable {
  StagingArea NOOP =
      new StagingArea() {
        @Override
        public void stage(Collection<StackTrace> stackTraces) {}

        @Override
        public void empty() {}
      };
  ConfigurableSupplier<StagingArea> SUPPLIER = new ConfigurableSupplier<>(NOOP);

  default void stage(StackTrace stackTrace) {
    stage(Collections.singleton(stackTrace));
  }

  void stage(Collection<StackTrace> stackTraces);

  void empty();

  default void close() {}
}
