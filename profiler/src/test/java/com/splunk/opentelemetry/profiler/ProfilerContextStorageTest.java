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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.events.ContextAttached;
import com.splunk.opentelemetry.profiler.events.JfrEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfilerContextStorageTest {

  String traceId;
  String spanId;
  Span span;
  Context newContext;
  SpanContext spanContext;
  @Mock ContextStorage delegate;
  @Mock Scope delegatedScope;

  @BeforeEach
  void setup() {
    traceId = TraceId.fromLongs(123, 455);
    spanId = SpanId.fromLong(23498);
    spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault());
    span = Span.wrap(spanContext);
    newContext = Context.root().with(span);
  }

  @Test
  void testNewEvent() {
    ContextAttached result = ProfilerContextStorage.newEvent(spanContext);
    assertEquals(traceId, result.getTraceId());
    assertEquals(spanId, result.getSpanId());
  }

  @Test
  void testAttachLifecycle() {
    JfrEvent inEvent = mock(JfrEvent.class);
    JfrEvent outEvent = mock(JfrEvent.class);
    Function<SpanContext, JfrEvent> newEvent = mock(Function.class);

    when(delegate.attach(newContext)).thenReturn(delegatedScope);
    when(inEvent.shouldCommit()).thenReturn(true);
    when(outEvent.shouldCommit()).thenReturn(true);
    when(newEvent.apply(spanContext)).thenReturn(inEvent);
    when(newEvent.apply(SpanContext.getInvalid())).thenReturn(outEvent);

    ProfilerContextStorage contextStorage = new ProfilerContextStorage(delegate, newEvent);
    contextStorage.setEnabled(true);
    contextStorage.setEmitJfrEvents(true);

    Scope resultScope = contextStorage.attach(newContext);
    verify(inEvent).begin();
    verify(inEvent).commit();
    verify(outEvent, never()).begin();
    verify(outEvent, never()).commit();
    verify(delegatedScope, never()).close();

    assertEquals(0, contextStorage.getActiveContextMap().approximateSize());

    resultScope.close(); // returns back to the initial/default span
    verify(outEvent).begin();
    verify(outEvent).commit();
    verify(delegatedScope).close();
  }

  @Test
  void testAttachWithInvalidContextDoesNotCreateAnyEvents() {
    spanContext = SpanContext.getInvalid();
    span = Span.wrap(spanContext);
    newContext = Context.root().with(span);

    Function<SpanContext, JfrEvent> newEvent =
        sc -> {
          fail("Should not have attempted to create events");
          throw new RuntimeException("boom");
        };

    when(delegate.attach(newContext)).thenReturn(delegatedScope);

    ProfilerContextStorage contextStorage = new ProfilerContextStorage(delegate, newEvent);
    contextStorage.setEmitJfrEvents(true);

    contextStorage.attach(newContext);
  }

  @Test
  void testCurrentSimplyDelegates() {
    Context expected = mock(Context.class);
    ContextStorage delegate = mock(ContextStorage.class);

    when(delegate.current()).thenReturn(expected);

    ProfilerContextStorage contextStorage = new ProfilerContextStorage(delegate);
    contextStorage.setEmitJfrEvents(true);
    contextStorage.setTrackActiveContext(true);

    Context result = contextStorage.current();
    assertEquals(expected, result);
  }

  @Test
  void testNotSampled() {
    Scope scope = mock(Scope.class);
    ContextStorage delegate = mock(ContextStorage.class);

    spanContext =
        SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());
    span = Span.wrap(spanContext);
    newContext = Context.root().with(span);

    when(delegate.attach(newContext)).thenReturn(scope);

    AtomicBoolean newEventWasCalled = new AtomicBoolean(false);
    Function<SpanContext, JfrEvent> newEvent =
        x -> {
          newEventWasCalled.set(true);
          return null;
        };

    ProfilerContextStorage contextStorage = new ProfilerContextStorage(delegate, newEvent);
    contextStorage.setEmitJfrEvents(true);
    Scope result = contextStorage.attach(newContext);

    assertEquals(scope, result);
    assertFalse(newEventWasCalled.get());
  }

  @Test
  void testActiveContextTracking() {
    Function<SpanContext, JfrEvent> newEvent =
        spanContext -> {
          throw new IllegalStateException("Should not have been called");
        };

    when(delegate.attach(newContext)).thenReturn(delegatedScope);

    ProfilerContextStorage contextStorage = new ProfilerContextStorage(delegate, newEvent);
    contextStorage.setEnabled(true);
    contextStorage.setTrackActiveContext(true);

    Scope resultScope = contextStorage.attach(newContext);
    verify(delegatedScope, never()).close();

    assertEquals(1, contextStorage.getActiveContextMap().approximateSize());

    resultScope.close(); // returns back to the initial/default span
    verify(delegatedScope).close();
  }
}
