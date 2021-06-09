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

package com.splunk.opentelemetry.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.events.ContextAttached;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JfrContextStorageTest {

  Span span;
  Context newContext;
  SpanContext spanContext;
  @Mock ContextStorage delegate;
  @Mock Scope delegatedScope;

  @BeforeEach
  void setup() {
    spanContext =
        SpanContext.create(
            TraceId.fromLongs(123, 455),
            SpanId.fromLong(23498),
            TraceFlags.getDefault(),
            TraceState.getDefault());
    span = Span.wrap(spanContext);
    newContext = Context.root().with(span);
  }

  @Test
  void testNewEvent() {
    String traceId = "nter108y";
    String spanId = "abc123";

    SpanContext context = mock(SpanContext.class);

    when(context.getSpanId()).thenReturn(spanId);
    when(context.getTraceId()).thenReturn(traceId);

    ContextAttached result = JfrContextStorage.newEvent(context, ContextAttached.OUT);
    assertEquals(traceId, result.getTraceId());
    assertEquals(spanId, result.getSpanId());
    assertEquals(ContextAttached.OUT, result.getDirection());
  }

  @Test
  void testAttachLifecycle() {
    ContextAttached inEvent = mock(ContextAttached.class);
    ContextAttached outEvent = mock(ContextAttached.class);
    BiFunction<SpanContext, Byte, ContextAttached> newEvent = mock(BiFunction.class);

    when(delegate.attach(newContext)).thenReturn(delegatedScope);
    when(inEvent.shouldCommit()).thenReturn(true);
    when(outEvent.shouldCommit()).thenReturn(true);
    when(newEvent.apply(spanContext, ContextAttached.IN)).thenReturn(inEvent);
    when(newEvent.apply(spanContext, ContextAttached.OUT)).thenReturn(outEvent);

    JfrContextStorage contextStorage = new JfrContextStorage(delegate, newEvent);

    Scope resultScope = contextStorage.attach(newContext);
    verify(inEvent).begin();
    verify(inEvent).commit();
    verify(newEvent, never()).apply(isA(SpanContext.class), eq(ContextAttached.OUT));
    verify(outEvent, never()).begin();
    verify(outEvent, never()).commit();
    verify(delegatedScope, never()).close();

    resultScope.close();
    verify(outEvent).begin();
    verify(outEvent).commit();
    verify(delegatedScope).close();
  }

  @Test
  void testAttachWithInvalidContextDoesNotCreateAnyEvents() {

    spanContext = SpanContext.getInvalid();
    span = Span.wrap(spanContext);
    newContext = Context.root().with(span);

    BiFunction<SpanContext, Byte, ContextAttached> newEvent =
        (sc, b) -> {
          fail("Should not have attempted to create events");
          throw new RuntimeException("boom");
        };

    when(delegate.attach(newContext)).thenReturn(delegatedScope);

    JfrContextStorage contextStorage = new JfrContextStorage(delegate, newEvent);

    contextStorage.attach(newContext);
  }

  @Test
  void testCurrentSimplyDelegates() {
    Context expected = mock(Context.class);
    ContextStorage delegate = mock(JfrContextStorage.class);

    when(delegate.current()).thenReturn(expected);

    JfrContextStorage contextStorage = new JfrContextStorage(delegate);
    Context result = contextStorage.current();
    assertEquals(expected, result);
  }
}
