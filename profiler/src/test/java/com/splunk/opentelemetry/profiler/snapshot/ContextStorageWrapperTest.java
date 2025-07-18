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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.opentelemetry.context.ContextStorage;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ContextStorageWrapperTest {
  private final ContextStorageRecorder delegate = new ContextStorageRecorder();
  private final ContextStorageWrapper activator = new ContextStorageWrapper(delegate);

  @Test
  void installThreadChangeDetector() {
    activator.wrapContextStorage(new TraceRegistry());
    assertInstanceOf(TraceThreadChangeDetector.class, delegate.storage);
  }

  @Test
  void installActiveSpanTracker() throws Exception {
    activator.wrapContextStorage(new TraceRegistry());

    var threadChangeDetector = (TraceThreadChangeDetector) delegate.storage;
    var field = threadChangeDetector.getClass().getDeclaredField("delegate");
    field.setAccessible(true);
    assertInstanceOf(ActiveSpanTracker.class, field.get(threadChangeDetector));
  }

  @Test
  void activateSpanTracker() {
    activator.wrapContextStorage(new TraceRegistry());
    assertInstanceOf(ActiveSpanTracker.class, SpanTracker.SUPPLIER.get());
  }

  private static class ContextStorageRecorder implements Consumer<UnaryOperator<ContextStorage>> {
    private ContextStorage storage = ContextStorage.defaultStorage();

    @Override
    public void accept(UnaryOperator<ContextStorage> operator) {
      storage = operator.apply(storage);
    }
  }
}
