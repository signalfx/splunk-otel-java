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

package com.splunk.opentelemetry.profiler.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.StackTracePredicate;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import org.junit.jupiter.api.Test;

class TracingStacksOnlyFilterTest {
  private final SpanLinkage linkage =
      new SpanLinkage(
          SpanContext.create(
              "deadbeefdeadbeefdeadbeefdeadbeef",
              "0123012301230123",
              TraceFlags.getDefault(),
              TraceState.getDefault()),
          15);
  private final String testStack = "\"pool-thread-1\" #15 daemon \nanything";

  @Test
  void testPassOnBothPass() {
    StackTracePredicate delegate = mock(StackTracePredicate.class);
    when(delegate.test(any(), anyInt(), anyInt())).thenReturn(true);

    SpanContextualizer contextualizer = mock(SpanContextualizer.class);
    when(contextualizer.link(any(), anyInt(), anyInt())).thenReturn(linkage);

    TracingStacksOnlyFilter filter = new TracingStacksOnlyFilter(delegate, contextualizer);
    assertTrue(filter.test(testStack, 0, testStack.length() - 1));
  }

  @Test
  void testFailOnDelegateFalse() {
    StackTracePredicate delegate = mock(StackTracePredicate.class);
    when(delegate.test(any(), anyInt(), anyInt())).thenReturn(true);

    SpanContextualizer contextualizer = mock(SpanContextualizer.class);
    when(contextualizer.link(any(), anyInt(), anyInt())).thenReturn(linkage);

    TracingStacksOnlyFilter filter = new TracingStacksOnlyFilter(delegate, contextualizer);
    assertTrue(filter.test(testStack, 0, testStack.length() - 1));
  }

  @Test
  void testFailOnContextMissing() {
    StackTracePredicate delegate = mock(StackTracePredicate.class);
    when(delegate.test(any(), anyInt(), anyInt())).thenReturn(true);

    SpanContextualizer contextualizer = mock(SpanContextualizer.class);
    when(contextualizer.link(any(), anyInt(), anyInt())).thenReturn(SpanLinkage.NONE);

    TracingStacksOnlyFilter filter = new TracingStacksOnlyFilter(delegate, contextualizer);
    assertFalse(filter.test(testStack, 0, testStack.length() - 1));
  }

  @Test
  void testPassCorrectParameters() {
    String stackInWall = "aaa" + testStack + "bbb";

    StackTracePredicate delegate = mock(StackTracePredicate.class);
    when(delegate.test(stackInWall, 3, 2 + testStack.length())).thenReturn(true);

    SpanContextualizer contextualizer = mock(SpanContextualizer.class);
    when(contextualizer.link(stackInWall, 3, 3 + testStack.length())).thenReturn(linkage);

    TracingStacksOnlyFilter filter = new TracingStacksOnlyFilter(delegate, contextualizer);
    assertTrue(filter.test(stackInWall, 3, 2 + testStack.length()));
  }
}
