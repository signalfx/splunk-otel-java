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

import io.opentelemetry.api.trace.SpanContext;
import java.io.Closeable;

interface StackTraceSampler extends Closeable {
  StackTraceSampler NOOP =
      new StackTraceSampler() {
        @Override
        public void start(Thread thread, String traceId) {}

        @Override
        public void stop(Thread thread) {}

        @Override
        public boolean isBeingSampled(Thread thread) {
          return false;
        }
      };
  ConfigurableSupplier<StackTraceSampler> SUPPLIER = new ConfigurableSupplier<>(NOOP);

  default void start(Thread thread, SpanContext spanContext) {
    start(thread, spanContext.getTraceId());
  }

  void start(Thread thread, String traceId);

  void stop(Thread thread);

  boolean isBeingSampled(Thread thread);

  default void close() {}
}
