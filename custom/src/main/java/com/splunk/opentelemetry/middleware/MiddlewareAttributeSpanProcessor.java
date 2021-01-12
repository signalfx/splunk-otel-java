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

package com.splunk.opentelemetry.middleware;

import com.splunk.opentelemetry.javaagent.bootstrap.MiddlewareHolder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class MiddlewareAttributeSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    String middlewareName = MiddlewareHolder.middlewareName.get();
    String middlewareVersion = MiddlewareHolder.middlewareVersion.get();

    if ((middlewareName == null && middlewareVersion == null)
        || span.getKind() != Span.Kind.SERVER) {
      return;
    }

    if (middlewareName != null) {
      span.setAttribute(MiddlewareAttributes.MIDDLEWARE_NAME.key, middlewareName);
    }
    if (middlewareVersion != null) {
      span.setAttribute(MiddlewareAttributes.MIDDLEWARE_VERSION.key, middlewareVersion);
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }
}
