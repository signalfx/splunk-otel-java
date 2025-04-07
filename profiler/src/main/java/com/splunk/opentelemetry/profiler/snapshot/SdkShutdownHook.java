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

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

class SdkShutdownHook implements SpanProcessor {
  @Override
  public CompletableResultCode shutdown() {
    List<CompletableResultCode> results = new ArrayList<>();
    results.add(close(StackTraceSampler.SUPPLIER.get()));
    results.add(close(StagingArea.SUPPLIER.get()));
    results.add(close(StackTraceExporter.SUPPLIER.get()));
    return CompletableResultCode.ofAll(results);
  }

  private CompletableResultCode close(Closeable closeable) {
    try {
      closeable.close();
      return CompletableResultCode.ofSuccess();
    } catch (Exception e) {
      return CompletableResultCode.ofExceptionalFailure(e);
    }
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {}

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
