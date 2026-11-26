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

import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.sampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.allocation.sampler.RateLimitingAllocationEventSampler;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.threaddump.StackTraceFilter;
import io.opentelemetry.api.trace.SpanContext;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;

public class TLABProcessor {
  public static final String NEW_TLAB_EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  public static final String OUTSIDE_TLAB_EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  public static final String ALLOCATION_SAMPLE_EVENT_NAME = "jdk.ObjectAllocationSample";

  private final boolean enabled;
  private final boolean useAllocationSampleEvent;
  private final EventReader eventReader;
  private final AllocationEventExporter allocationEventExporter;
  private final AllocationEventSampler sampler;
  private final SpanContextualizer spanContextualizer;
  private final StackTraceFilter stackTraceFilter;

  private TLABProcessor(Builder builder) {
    this.enabled = builder.enabled;
    this.useAllocationSampleEvent = builder.useAllocationSampleEvent;
    this.eventReader = builder.eventReader;
    this.allocationEventExporter = builder.allocationEventExporter;
    this.sampler = builder.sampler;
    this.spanContextualizer = builder.spanContextualizer;
    this.stackTraceFilter = builder.stackTraceFilter;
  }

  public void accept(IItem event) {
    // JFR shares events across recordings. Process only the allocation events we enabled.
    if (!enabled || !isEnabledEvent(event.getType().getIdentifier())) {
      return;
    }
    IMCStackTrace stackTrace = eventReader.getStackTrace(event);
    if (stackTrace == null) {
      return;
    }

    if (stackTraceFilter != null && !stackTraceFilter.test(event)) {
      return;
    }
    // Discard events not chosen by the sampling strategy
    if (sampler != null && !sampler.shouldSample(event)) {
      return;
    }

    SpanContext spanContext = null;
    IMCThread thread = eventReader.getThread(event);
    if (thread != null && thread.getThreadId() != null) {
      spanContext = spanContextualizer.link(thread.getThreadId()).getSpanContext();
    }

    allocationEventExporter.export(event, sampler, spanContext);
  }

  private boolean isEnabledEvent(String eventName) {
    return useAllocationSampleEvent
        ? ALLOCATION_SAMPLE_EVENT_NAME.equals(eventName)
        : isTlabEvent(eventName);
  }

  static boolean isTlabEvent(String eventName) {
    return NEW_TLAB_EVENT_NAME.equals(eventName) || OUTSIDE_TLAB_EVENT_NAME.equals(eventName);
  }

  public void flush() {
    allocationEventExporter.flush();
  }

  public AllocationEventSampler getAllocationEventSampler() {
    return sampler;
  }

  static Builder builder(ProfilerConfiguration config) {
    boolean useAllocationSampleEvent = config.isNativeAllocationSamplingEnabled();
    Builder builder = new Builder(config.getMemoryEnabled(), useAllocationSampleEvent);
    if (config.getMemoryEventRateLimitEnabled() && !useAllocationSampleEvent) {
      String rateLimit = config.getMemoryEventRate();
      builder.sampler(new RateLimitingAllocationEventSampler(rateLimit));
    }
    return builder;
  }

  static class Builder {
    private final boolean enabled;
    private final boolean useAllocationSampleEvent;
    private EventReader eventReader;
    private AllocationEventExporter allocationEventExporter;
    private AllocationEventSampler sampler;
    private SpanContextualizer spanContextualizer;
    private StackTraceFilter stackTraceFilter;

    Builder(boolean enabled, boolean useAllocationSampleEvent) {
      this.enabled = enabled;
      this.useAllocationSampleEvent = useAllocationSampleEvent;
    }

    TLABProcessor build() {
      return new TLABProcessor(this);
    }

    Builder eventReader(EventReader eventReader) {
      this.eventReader = eventReader;
      return this;
    }

    Builder allocationEventExporter(AllocationEventExporter allocationEventExporter) {
      this.allocationEventExporter = allocationEventExporter;
      return this;
    }

    Builder sampler(AllocationEventSampler sampler) {
      this.sampler = sampler;
      return this;
    }

    Builder spanContextualizer(SpanContextualizer spanContextualizer) {
      this.spanContextualizer = spanContextualizer;
      return this;
    }

    Builder stackTraceFilter(StackTraceFilter stackTraceFilter) {
      this.stackTraceFilter = stackTraceFilter;
      return this;
    }
  }
}
