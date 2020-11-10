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

package com.splunk.opentelemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InstrumentationLibrarySpanProcessorTest {

  @Test
  void shouldAddInstrumentationLibrary() {
    ReadWriteSpanWithLibrary span = new ReadWriteSpanWithLibrary();
    InstrumentationLibrarySpanProcessor processor = new InstrumentationLibrarySpanProcessor();
    processor.onStart(Context.root(), span);

    Assertions.assertEquals(
        "com.splunk.test",
        span.attributes.get(AttributeKey.stringKey("splunk.instrumentation_library.name")));
    Assertions.assertEquals(
        "1.2.3",
        span.attributes.get(AttributeKey.stringKey("splunk.instrumentation_library.version")));
  }

  private static class ReadWriteSpanWithLibrary implements ReadWriteSpan {

    private Attributes attributes = Attributes.empty();

    @Override
    public SpanContext getSpanContext() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public SpanData toSpanData() {
      return null;
    }

    @Override
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
      return InstrumentationLibraryInfo.create("com.splunk.test", "1.2.3");
    }

    @Override
    public boolean hasEnded() {
      return false;
    }

    @Override
    public long getLatencyNanos() {
      return 0;
    }

    @Override
    public Span setAttribute(String s, String s1) {
      attributes = attributes.toBuilder().put(s, s1).build();
      return this;
    }

    @Override
    public Span setAttribute(String s, long l) {
      return this;
    }

    @Override
    public Span setAttribute(String s, double v) {
      return this;
    }

    @Override
    public Span setAttribute(String s, boolean b) {
      return this;
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
      return this;
    }

    @Override
    public Span addEvent(String s) {
      return this;
    }

    @Override
    public Span addEvent(String s, long l) {
      return this;
    }

    @Override
    public Span addEvent(String s, Attributes attributes) {
      return this;
    }

    @Override
    public Span addEvent(String s, Attributes attributes, long l) {
      return this;
    }

    @Override
    public Span setStatus(StatusCode canonicalCode) {
      return this;
    }

    @Override
    public Span setStatus(StatusCode canonicalCode, String description) {
      return this;
    }

    @Override
    public Span recordException(Throwable throwable) {
      return this;
    }

    @Override
    public Span recordException(Throwable throwable, Attributes attributes) {
      return this;
    }

    @Override
    public Span updateName(String s) {
      return this;
    }

    @Override
    public void end() {}

    @Override
    public void end(long timestamp) {}

    @Override
    public boolean isRecording() {
      return false;
    }
  }
}
