package com.signalfx.opentelemetry;

import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanId;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.TraceFlags;
import io.opentelemetry.trace.TraceId;
import io.opentelemetry.trace.TraceState;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SpanDataWrapperTest {

  @Test
  void shouldAddInstrumentationLibrary() {
    SpanData spanData = new SpanDataWithLibrary();
    SpanDataWrapper wrapper = new SpanDataWrapper(spanData);

    Assertions.assertEquals("com.signalfx.test", wrapper.getAttributes().get("signalfx.instrumentation_library.name").getStringValue());
    Assertions.assertEquals("1.2.3", wrapper.getAttributes().get("signalfx.instrumentation_library.version").getStringValue());
  }

  private static class SpanDataWithLibrary implements SpanData {

    @Override
    public TraceId getTraceId() {
      return null;
    }

    @Override
    public SpanId getSpanId() {
      return null;
    }

    @Override
    public TraceFlags getTraceFlags() {
      return null;
    }

    @Override
    public TraceState getTraceState() {
      return null;
    }

    @Override
    public SpanId getParentSpanId() {
      return null;
    }

    @Override
    public Resource getResource() {
      return null;
    }

    @Override
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
      return InstrumentationLibraryInfo.create("com.signalfx.test", "1.2.3");
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public Span.Kind getKind() {
      return null;
    }

    @Override
    public long getStartEpochNanos() {
      return 0;
    }

    @Override
    public ReadableAttributes getAttributes() {
      return Attributes.empty();
    }

    @Override
    public List<Event> getEvents() {
      return null;
    }

    @Override
    public List<Link> getLinks() {
      return null;
    }

    @Override
    public Status getStatus() {
      return null;
    }

    @Override
    public long getEndEpochNanos() {
      return 0;
    }

    @Override
    public boolean getHasRemoteParent() {
      return false;
    }

    @Override
    public boolean getHasEnded() {
      return false;
    }

    @Override
    public int getTotalRecordedEvents() {
      return 0;
    }

    @Override
    public int getTotalRecordedLinks() {
      return 0;
    }

    @Override
    public int getTotalAttributeCount() {
      return 0;
    }
  }
}