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

package com.splunk.opentelemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Until all Splunk customers migrate to OTLP, we add our own custom span attributes to signal
 * instrumentation library name and version.
 *
 * <p>As a side note, official OTLP->Zipkin and OTLP->Jaeger translations already specify similar
 * conversion, see
 * https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/sdk_exporters/zipkin.md#instrumentationlibrary
 * and
 * https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/sdk_exporters/jaeger.md#instrumentationlibrary
 * such translations are not released yet to the Collector. When they reach main branch, this
 * processor can be deprecated.
 */
public class InstrumentationLibrarySpanProcessor implements SpanProcessor {
  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    InstrumentationLibraryInfo libraryInfo = span.getInstrumentationLibraryInfo();

    span.setAttribute("splunk.instrumentation_library.name", libraryInfo.getName());
    if (libraryInfo.getVersion() != null) {
      span.setAttribute("splunk.instrumentation_library.version", libraryInfo.getVersion());
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
