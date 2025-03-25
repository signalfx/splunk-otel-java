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
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.OnStartSpanProcessor;
import io.opentelemetry.sdk.trace.ReadWriteSpan;

public class AppdBonusSpanProcessor implements OnStartSpanProcessor.OnStart {

  static final AttributeKey<String> APPD_ATTR_ACCT = stringKey("appd.upstream.account.id");
  static final AttributeKey<String> APPD_ATTR_APP = stringKey("appd.upstream.app.id");
  static final AttributeKey<String> APPD_ATTR_BT = stringKey("appd.upstream.bt.id");
  static final AttributeKey<String> APPD_ATTR_TIER = stringKey("appd.upstream.tier.id");

  @Override
  public void apply(Context context, ReadWriteSpan span) {
    AppdBonusContext ctx = context.get(CONTEXT_KEY);
    if (ctx == null) {
      return;
    }
    // Set attributes only for the local root span
    SpanContext parentSpanContext = span.getParentSpanContext();
    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      if (ctx.getAccountId() != null) {
        span.setAttribute(APPD_ATTR_ACCT, ctx.getAccountId());
      }
      if (ctx.getAppId() != null) {
        span.setAttribute(APPD_ATTR_APP, ctx.getAppId());
      }
      if (ctx.getBusinessTransactionId() != null) {
        span.setAttribute(APPD_ATTR_BT, ctx.getBusinessTransactionId());
      }
      if (ctx.getTierId() != null) {
        span.setAttribute(APPD_ATTR_TIER, ctx.getTierId());
      }
    }
  }
}
