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

package com.splunk.opentelemetry.profiler.events;

import static com.splunk.opentelemetry.profiler.events.ContextAttached.EVENT_NAME;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(EVENT_NAME)
@Label("otel context attached")
@Category("opentelemetry")
@StackTrace(false)
public class ContextAttached extends Event {

  public static final String EVENT_NAME = "otel.ContextAttached";

  public final int traceFlags;
  public final String traceId;
  public final String spanId;

  public ContextAttached(String traceId, String spanId) {
    this(0, traceId, spanId);
  }

  public ContextAttached(int traceFlags, String traceId, String spanId) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.traceFlags = traceFlags;
  }

  public int getTraceFlags() {
    return traceFlags;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }
}
