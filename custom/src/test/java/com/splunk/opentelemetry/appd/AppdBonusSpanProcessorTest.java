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

package com.splunk.opentelemetry.appd;

import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CONTEXT_KEY;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_ACCT;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_APP;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_BT;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_TIER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;

class AppdBonusSpanProcessorTest {

  @Test
  void shouldSetAppdAttributesOnRootSpan() {
    // Given
    Context context = mock();
    AppdBonusContext appdContext = new AppdBonusContext("myacct", "myapp", "mybt", "mytier");
    when(context.get(CONTEXT_KEY)).thenReturn(appdContext);

    ReadWriteSpan span = mock();

    AppdBonusSpanProcessor testClass = new AppdBonusSpanProcessor();

    // When
    testClass.apply(context, span);

    // Then
    verify(span).setAttribute(APPD_ATTR_ACCT, "myacct");
    verify(span).setAttribute(APPD_ATTR_APP, "myapp");
    verify(span).setAttribute(APPD_ATTR_BT, "mybt");
    verify(span).setAttribute(APPD_ATTR_TIER, "mytier");
  }

  @Test
  void shouldNotSetAppdAttributesOnNestedSpans() {
    // Given
    Context context = mock();
    AppdBonusContext appdContext = new AppdBonusContext("myacct", "myapp", "mybt", "mytier");
    when(context.get(CONTEXT_KEY)).thenReturn(appdContext);

    ReadWriteSpan span = mock();
    SpanContext parentSpanContext = mock(SpanContext.class);
    when(span.getParentSpanContext()).thenReturn(parentSpanContext);

    AppdBonusSpanProcessor testClass = new AppdBonusSpanProcessor();

    // When
    testClass.apply(context, span);

    // Then
    verify(span, never()).setAttribute(APPD_ATTR_ACCT, "myacct");
    verify(span, never()).setAttribute(APPD_ATTR_APP, "myapp");
    verify(span, never()).setAttribute(APPD_ATTR_BT, "mybt");
    verify(span, never()).setAttribute(APPD_ATTR_TIER, "mytier");
  }
}
