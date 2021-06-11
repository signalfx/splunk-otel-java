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

@Name(EVENT_NAME)
@Label("otel context attached")
@Category("opentelemetry")
public class ContextAttached extends Event {

  public static final String EVENT_NAME = "otel.ContextAttached";
  // Context is starting
  public static final byte IN = 0;
  // Context is ending/closing
  public static final byte OUT = 1;

  public final String traceId;
  public final String spanId;
  public final byte direction;

  public ContextAttached(String traceId, String spanId, byte direction) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.direction = direction;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  public byte getDirection() {
    return direction;
  }
}
