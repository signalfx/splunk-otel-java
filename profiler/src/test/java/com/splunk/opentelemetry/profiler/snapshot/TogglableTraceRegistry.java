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

import io.opentelemetry.api.trace.SpanContext;
import java.util.HashSet;
import java.util.Set;

class TogglableTraceRegistry implements TraceRegistry {
  private final Set<String> traceIds = new HashSet<>();

  enum State {
    ON,
    OFF
  }

  private State state = State.ON;

  @Override
  public void register(SpanContext spanContext) {
    if (state == State.ON) {
      traceIds.add(spanContext.getTraceId());
    }
  }

  public void toggle(State state) {
    this.state = state;
  }

  @Override
  public boolean isRegistered(SpanContext spanContext) {
    return traceIds.contains(spanContext.getTraceId());
  }

  @Override
  public void unregister(SpanContext spanContext) {
    traceIds.remove(spanContext.getTraceId());
  }
}
