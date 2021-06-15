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
import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nullable;

/** Data object for OpenTelemetry logs */
public class LogEntry {

  private final String name;
  private final Attributes attributes;
  private final Instant time;
  private final String body;
  @Nullable private final String traceId;
  @Nullable private final String spanId;

  private LogEntry(Builder builder) {
    this.name = builder.name;
    this.attributes = builder.attributes;
    this.time = builder.time;
    this.body = builder.body;
    this.traceId = builder.traceId;
    this.spanId = builder.spanId;
  }

  public String getName() {
    return name;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public String getBody() {
    return body;
  }

  public Instant getTime() {
    return time;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private Attributes attributes = Attributes.empty();
    private Instant time;
    private String body;
    private String traceId;
    private String spanId;

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

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public Builder traceId(String traceId) {
      this.traceId = traceId;
      return this;
    }

    public Builder spanId(String spanId) {
      this.spanId = spanId;
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
        && Objects.equals(traceId, logEntry.traceId)
        && Objects.equals(spanId, logEntry.spanId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, attributes, time, body, traceId, spanId);
  }

  @Override
  public String toString() {
    return "LogEntry{"
        + "name='"
        + name
        + '\''
        + ", attributes="
        + attributes
        + ", time="
        + time
        + ", body='"
        + body
        + '\''
        + ", traceId='"
        + traceId
        + '\''
        + ", spanId='"
        + spanId
        + '\''
        + '}';
  }
}
