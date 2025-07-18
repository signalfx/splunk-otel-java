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
        public void start(Thread thread, SpanContext spanContext) {}

        @Override
        public void stop(Thread thread, SpanContext spanContext) {}

        @Override
        public void stopAllSampling(SpanContext spanContext) {}

        @Override
        public boolean isBeingSampled(Thread thread) {
          return false;
        }
      };
  ConfigurableSupplier<StackTraceSampler> SUPPLIER = new ConfigurableSupplier<>(NOOP);

  /**
   * Start trace sampling of the provided {@link Thread}, associating that {@link Thread}
   * with the provided {@link SpanContext}.
   *
   * @param thread {@link Thread} to sample
   * @param spanContext {@link SpanContext} to associate with the {@link Thread}
   */
  void start(Thread thread, SpanContext spanContext);

  /**
   * Stop sampling the {@link Thread}, assuming the provided {@link SpanContext} is
   * associated with the {@link Thread}.
   *
   * @param thread {@link Thread} to stop sampling
   * @param spanContext {@link SpanContext} to associate with the {@link Thread}
   */
  void stop(Thread  thread, SpanContext spanContext);

  /**
   * Stops sampling of all threads associated with {@link SpanContext}.
   */
  void stopAllSampling(SpanContext spanContext);

  boolean isBeingSampled(Thread thread);

  default void close() {}
}
