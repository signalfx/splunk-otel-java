/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.opentelemetry.profiler.exporter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * In memory implementation of the OpenTelemetry {@link Logger} interface that allows for direct
 * access to the collected logs. Intended for testing use only.
 */
class InMemoryOtelLogger implements Logger, AfterEachCallback {
  private final List<LogRecordData> records = new ArrayList<>();

  @Override
  public LogRecordBuilder logRecordBuilder() {
    return new Builder(this);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    records.clear();
  }

  List<LogRecordData> records() {
    return Collections.unmodifiableList(records);
  }

  static class Builder implements LogRecordBuilder {
    private final InMemoryOtelLogger logger;
    private long timestampEpochNanos;
    private long observedTimestampEpochNanos;
    private Context context = Context.current();
    private Severity severity = Severity.UNDEFINED_SEVERITY_NUMBER;
    private String severityText = "";
    private String body = "";
    private final AttributesBuilder attributes = Attributes.builder();

    private Builder(InMemoryOtelLogger logger) {
      this.logger = logger;
    }

    @Override
    public LogRecordBuilder setTimestamp(Instant instant) {
      return setTimestamp(instant.toEpochMilli(), TimeUnit.MILLISECONDS);
    }

    @Override
    public LogRecordBuilder setTimestamp(long timestamp, TimeUnit unit) {
      this.timestampEpochNanos = unit.toNanos(timestamp);
      return this;
    }

    @Override
    public LogRecordBuilder setObservedTimestamp(Instant instant) {
      return setObservedTimestamp(instant.toEpochMilli(), TimeUnit.MILLISECONDS);
    }

    @Override
    public LogRecordBuilder setObservedTimestamp(long timestamp, TimeUnit unit) {
      this.observedTimestampEpochNanos = unit.toNanos(timestamp);
      return this;
    }

    @Override
    public LogRecordBuilder setContext(Context context) {
      this.context = Objects.requireNonNull(context);
      return this;
    }

    @Override
    public LogRecordBuilder setSeverity(Severity severity) {
      this.severity = Objects.requireNonNull(severity);
      return this;
    }

    @Override
    public LogRecordBuilder setSeverityText(String severityText) {
      this.severityText = Objects.requireNonNull(severityText);
      return this;
    }

    @Override
    public LogRecordBuilder setBody(String body) {
      this.body = Objects.requireNonNull(body);
      return this;
    }

    @Override
    public <T> LogRecordBuilder setAttribute(AttributeKey<T> key, T value) {
      attributes.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
      return this;
    }

    @Override
    public void emit() {
      logger.records.add(
          new LogRecord(
              null,
              null,
              timestampEpochNanos,
              observedTimestampEpochNanos,
              Span.fromContext(context).getSpanContext(),
              severity,
              severityText,
              Body.string(body),
              attributes.build()));
    }
  }

  static class LogRecord implements LogRecordData {
    private final Resource resource;
    private final InstrumentationScopeInfo instrumentationScopeInfo;
    private final long timestampEpochNanos;
    private final long observedTimestampEpochNanos;
    private final SpanContext spanContext;
    private final Severity severity;
    private final String severityText;
    private final Body body;
    private final Attributes attributes;

    LogRecord(
        Resource resource,
        InstrumentationScopeInfo instrumentationScopeInfo,
        long timestampEpochNanos,
        long observedTimestampEpochNanos,
        SpanContext spanContext,
        Severity severity,
        String severityText,
        Body body,
        Attributes attributes) {
      this.resource = resource;
      this.instrumentationScopeInfo = instrumentationScopeInfo;
      this.timestampEpochNanos = timestampEpochNanos;
      this.observedTimestampEpochNanos = observedTimestampEpochNanos;
      this.spanContext = spanContext;
      this.severity = severity;
      this.severityText = severityText;
      this.body = body;
      this.attributes = attributes;
    }

    @Override
    public Resource getResource() {
      return resource;
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
      return instrumentationScopeInfo;
    }

    @Override
    public long getTimestampEpochNanos() {
      return timestampEpochNanos;
    }

    @Override
    public long getObservedTimestampEpochNanos() {
      return observedTimestampEpochNanos;
    }

    @Override
    public SpanContext getSpanContext() {
      return spanContext;
    }

    @Override
    public Severity getSeverity() {
      return severity;
    }

    @Override
    public String getSeverityText() {
      return severityText;
    }

    @Override
    public Body getBody() {
      return body;
    }

    @Override
    public Attributes getAttributes() {
      return attributes;
    }

    @Override
    public int getTotalAttributeCount() {
      return attributes.size();
    }
  }
}
