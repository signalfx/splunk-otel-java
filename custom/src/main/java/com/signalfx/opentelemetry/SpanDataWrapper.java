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

public class SpanDataWrapper implements SpanData {
  private final SpanData delegate;
  private final Attributes attributes;

  public SpanDataWrapper(SpanData delegate) {
    this.delegate = delegate;
    Attributes.Builder builder = Attributes.newBuilder();
    delegate.getAttributes().forEach(builder::setAttribute);
    InstrumentationLibraryInfo libraryInfo = delegate.getInstrumentationLibraryInfo();
    builder.setAttribute("library", libraryInfo.getName());
    if (libraryInfo.getVersion() != null) {
      builder.setAttribute("libraryVersion", libraryInfo.getVersion());
    }
    attributes = builder.build();
  }

  @Override
  public TraceId getTraceId() {
    return delegate.getTraceId();
  }

  @Override
  public SpanId getSpanId() {
    return delegate.getSpanId();
  }

  @Override
  public TraceFlags getTraceFlags() {
    return delegate.getTraceFlags();
  }

  @Override
  public TraceState getTraceState() {
    return delegate.getTraceState();
  }

  @Override
  public SpanId getParentSpanId() {
    return delegate.getParentSpanId();
  }

  @Override
  public Resource getResource() {
    return delegate.getResource();
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return delegate.getInstrumentationLibraryInfo();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public Span.Kind getKind() {
    return delegate.getKind();
  }

  @Override
  public long getStartEpochNanos() {
    return delegate.getStartEpochNanos();
  }

  @Override
  public ReadableAttributes getAttributes() {
    return attributes;
  }

  @Override
  public List<Event> getEvents() {
    return delegate.getEvents();
  }

  @Override
  public List<Link> getLinks() {
    return delegate.getLinks();
  }

  @Override
  public Status getStatus() {
    return delegate.getStatus();
  }

  @Override
  public long getEndEpochNanos() {
    return delegate.getEndEpochNanos();
  }

  @Override
  public boolean getHasRemoteParent() {
    return delegate.getHasRemoteParent();
  }

  @Override
  public boolean getHasEnded() {
    return delegate.getHasEnded();
  }

  @Override
  public int getTotalRecordedEvents() {
    return delegate.getTotalRecordedEvents();
  }

  @Override
  public int getTotalRecordedLinks() {
    return delegate.getTotalRecordedLinks();
  }

  @Override
  public int getTotalAttributeCount() {
    return delegate.getTotalAttributeCount() + 2;
  }
}
