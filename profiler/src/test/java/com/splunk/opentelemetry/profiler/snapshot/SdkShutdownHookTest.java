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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanContext;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class SdkShutdownHookTest {
  private final ClosingObserver observer = new ClosingObserver();
  private final SdkShutdownHook shutdownHook = new SdkShutdownHook();

  @Test
  void shutdownStackTraceSampling() {
    try {
      StackTraceSampler.SUPPLIER.configure(observer);
      shutdownHook.shutdown();
      assertThat(observer.isClosed).isTrue();
    } finally {
      StackTraceSampler.SUPPLIER.reset();
    }
  }

  @Test
  void shutdownStagingArea() {
    try {
      StagingArea.SUPPLIER.configure(observer);
      shutdownHook.shutdown();
      assertThat(observer.isClosed).isTrue();
    } finally {
      StagingArea.SUPPLIER.reset();
    }
  }

  @Test
  void shutdownStackTraceExporting() {
    try {
      StackTraceExporter.SUPPLIER.configure(observer);
      shutdownHook.shutdown();
      assertThat(observer.isClosed).isTrue();
    } finally {
      StackTraceExporter.SUPPLIER.reset();
    }
  }

  private static class ClosingObserver
      implements StackTraceSampler, StagingArea, StackTraceExporter {
    private boolean isClosed = false;

    @Override
    public void close() {
      isClosed = true;
    }

    @Override
    public void start(Thread thread, SpanContext spanContext) {}

    @Override
    public void stop(Thread thread, SpanContext spanContext) {}

    @Override
    public void stage(Collection<StackTrace> stackTraces) {}

    @Override
    public void empty() {}

    @Override
    public void export(Collection<StackTrace> stackTraces) {}
  }
}
