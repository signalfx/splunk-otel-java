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

import static com.splunk.opentelemetry.appd.AppdBonusConstants.APPD_ATTR_ACCT;
import static com.splunk.opentelemetry.appd.AppdBonusConstants.APPD_ATTR_APP;
import static com.splunk.opentelemetry.appd.AppdBonusConstants.APPD_ATTR_BT;
import static com.splunk.opentelemetry.appd.AppdBonusConstants.APPD_ATTR_TIER;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CONTEXT_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;

class AppdBonusSpanProcessorTest {

  @Test
  void onStart() {
    Context context = mock();

    AppdBonusContext appdContext = new AppdBonusContext("myacct", "myapp", "mybt", "mytier");
    ReadWriteSpan span = mock();

    when(context.get(CONTEXT_KEY)).thenReturn(appdContext);

    AppdBonusSpanProcessor testClass = new AppdBonusSpanProcessor();
    testClass.apply(context, span);

    verify(span).setAttribute(APPD_ATTR_ACCT, "myacct");
    verify(span).setAttribute(APPD_ATTR_APP, "myapp");
    verify(span).setAttribute(APPD_ATTR_BT, "mybt");
    verify(span).setAttribute(APPD_ATTR_TIER, "mytier");
  }
}
