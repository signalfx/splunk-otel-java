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

package com.splunk.opentelemetry.profiler;

import static com.splunk.opentelemetry.profiler.LogDataCreator.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;

import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.time.Duration;

public class LogDataCommonAttributes {

  private final EventPeriods periods;

  public LogDataCommonAttributes(EventPeriods periods) {
    this.periods = periods;
  }

  Attributes build(StackToSpanLinkage linkedStack) {
    return build(linkedStack.getSourceEventName());
  }

  Attributes build(String eventName) {
    Duration eventPeriod = periods.getDuration(eventName);

    // Note: It is currently believed that the span id and trace id on the LogRecord itself
    // do not get ingested correctly. Placing them here as attributes is a temporary workaround
    // until the collector/ingest can be remedied.

    AttributesBuilder builder =
        Attributes.builder().put(SOURCE_TYPE, PROFILING_SOURCE).put(SOURCE_EVENT_NAME, eventName);

    if (!EventPeriods.UNKNOWN.equals(eventPeriod)) {
      builder.put(SOURCE_EVENT_PERIOD, eventPeriod.toMillis());
    }
    return builder.build();
  }
}
