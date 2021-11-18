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

package com.splunk.opentelemetry.logs;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.Severity;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/** Data object for OpenTelemetry logs */
public class LogEntry implements LogData {

  private final String name;
  private final Attributes attributes;
  private final Instant time;
  private final Body body;
  private final SpanContext spanContext;
  private final Resource resource;
  private final InstrumentationLibraryInfo instrumentationLibrary;

  private LogEntry(Builder builder) {
    this.name = builder.name;
    this.attributes = builder.attributes;
    this.time = builder.time;
    this.body = builder.body;
    this.spanContext = builder.spanContext;
    this.resource = builder.resource;
    this.instrumentationLibrary = builder.instrumentationLibrary;
  }

  @Override
  public Resource getResource() {
    return resource;
  }

  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return instrumentationLibrary;
  }

  @Override
  public long getEpochNanos() {
    return TimeUnit.MILLISECONDS.toNanos(time.toEpochMilli());
  }

  @Override
  public SpanContext getSpanContext() {
    return spanContext;
  }

  @Override
  public Severity getSeverity() {
    return null;
  }

  @Nullable
  @Override
  public String getSeverityText() {
    return null;
  }

  public String getName() {
    return name;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public Body getBody() {
    return body;
  }

  public String getBodyAsString() {
    return body.asString();
  }

  public Instant getTime() {
    return time;
  }

  public String getTraceId() {
    return spanContext.getTraceId();
  }

  public String getSpanId() {
    return spanContext.getSpanId();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private Attributes attributes = Attributes.empty();
    private Instant time = Instant.EPOCH;
    private Body body = Body.empty();
    private SpanContext spanContext = SpanContext.getInvalid();
    public InstrumentationLibraryInfo instrumentationLibrary = InstrumentationLibraryInfo.empty();
    public Resource resource = Resource.getDefault();

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder attributes(Attributes attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder time(Instant time) {
      this.time = time;
      return this;
    }

    public Builder body(Body body) {
      this.body = body;
      return this;
    }

    public Builder bodyString(String body) {
      this.body = Body.string(body);
      return this;
    }

    public Builder spanContext(SpanContext spanContext) {
      this.spanContext = spanContext;
      return this;
    }

    public Builder resource(Resource resource) {
      this.resource = resource;
      return this;
    }

    public Builder instrumentationLibrary(InstrumentationLibraryInfo instrumentationLibrary) {
      this.instrumentationLibrary = instrumentationLibrary;
      return this;
    }

    public LogEntry build() {
      return new LogEntry(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LogEntry logEntry = (LogEntry) o;
    return name.equals(logEntry.name)
        && attributes.equals(logEntry.attributes)
        && time.equals(logEntry.time)
        && body.equals(logEntry.body)
        && Objects.equals(spanContext, logEntry.spanContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, attributes, time, body, spanContext);
  }
}
