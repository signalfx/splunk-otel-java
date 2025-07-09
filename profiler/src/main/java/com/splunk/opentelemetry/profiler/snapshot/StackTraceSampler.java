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
        public void start(SpanContext spanContext) {}

        @Override
        public void stop(String traceId, String spanId) {}
      };
  ConfigurableSupplier<StackTraceSampler> SUPPLIER = new ConfigurableSupplier<>(NOOP);

  void start(SpanContext spanContext);

  default void stop(SpanContext spanContext) {
    stop(spanContext.getTraceId(), spanContext.getSpanId());
  }

  void stop(String traceId, String spanId);

  default void close() {}
}
