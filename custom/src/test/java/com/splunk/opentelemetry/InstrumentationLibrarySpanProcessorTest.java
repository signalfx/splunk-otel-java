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

import io.opentelemetry.common.AttributeKey;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.EndSpanOptions;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.StatusCanonicalCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InstrumentationLibrarySpanProcessorTest {

  @Test
  void shouldAddInstrumentationLibrary() {
    ReadWriteSpanWithLibrary span = new ReadWriteSpanWithLibrary();
    InstrumentationLibrarySpanProcessor processor = new InstrumentationLibrarySpanProcessor();
    processor.onStart(span);

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
    public void setAttribute(String s, String s1) {
      attributes = attributes.toBuilder().setAttribute(s, s1).build();
    }

    @Override
    public void setAttribute(String s, long l) {}

    @Override
    public void setAttribute(String s, double v) {}

    @Override
    public void setAttribute(String s, boolean b) {}

    @Override
    public <T> void setAttribute(AttributeKey<T> key, T value) {}

    @Override
    public void addEvent(String s) {}

    @Override
    public void addEvent(String s, long l) {}

    @Override
    public void addEvent(String s, Attributes attributes) {}

    @Override
    public void addEvent(String s, Attributes attributes, long l) {}

    @Override
    public void setStatus(StatusCanonicalCode canonicalCode) {}

    @Override
    public void setStatus(StatusCanonicalCode canonicalCode, String description) {}

    @Override
    public void recordException(Throwable throwable) {}

    @Override
    public void recordException(Throwable throwable, Attributes attributes) {}

    @Override
    public void updateName(String s) {}

    @Override
    public void end() {}

    @Override
    public void end(EndSpanOptions endSpanOptions) {}

    @Override
    public SpanContext getContext() {
      return null;
    }

    @Override
    public boolean isRecording() {
      return false;
    }
  }
}
