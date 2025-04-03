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

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.context.ContextStorage;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

class InterceptingContextStorageSpanTrackingActivator implements SpanTrackingActivator {
  private final Consumer<UnaryOperator<ContextStorage>> contextStorageWrappingFunction;

  InterceptingContextStorageSpanTrackingActivator() {
    this(ContextStorage::addWrapper);
  }

  @VisibleForTesting
  InterceptingContextStorageSpanTrackingActivator(
      Consumer<UnaryOperator<ContextStorage>> contextStorageWrappingFunction) {
    this.contextStorageWrappingFunction = contextStorageWrappingFunction;
  }

  @Override
  public void activate(TraceRegistry registry) {
    contextStorageWrappingFunction.accept(
        contextStorage -> {
          ActiveSpanTracker tracker = new ActiveSpanTracker(contextStorage, registry);
          SpanTrackerProvider.INSTANCE.configure(tracker);
          return tracker;
        });
  }
}
