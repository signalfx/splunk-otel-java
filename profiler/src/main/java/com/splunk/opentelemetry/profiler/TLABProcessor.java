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
import com.splunk.opentelemetry.profiler.allocation.sampler.SystematicAllocationEventSampler;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.api.config.Config;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

public class TLABProcessor implements Consumer<RecordedEvent> {
  public static final String NEW_TLAB_EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  public static final String OUTSIDE_TLAB_EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  static final AttributeKey<Long> ALLOCATION_SIZE_KEY = AttributeKey.longKey("memory.allocated");

  private final boolean enabled;
  private final AllocationEventExporter allocationEventExporter;
  private final AllocationEventSampler sampler;
  private final SpanContextualizer spanContextualizer;
  private final StackTraceFilter stackTraceFilter;

  private TLABProcessor(Builder builder) {
    this.enabled = builder.enabled;
    this.allocationEventExporter = builder.allocationEventExporter;
    this.sampler = builder.sampler;
    this.spanContextualizer = builder.spanContextualizer;
    this.stackTraceFilter = builder.stackTraceFilter;
  }

  @Override
  public void accept(RecordedEvent event) {
    // If there is another JFR recording in progress that has enabled TLAB events we might also get
    // them because JFR sends all enabled events to all recordings, if that is the case ignore them.
    if (!enabled) {
      return;
    }
    RecordedStackTrace stackTrace = event.getStackTrace();
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
    RecordedThread thread = event.getThread();
    if (thread != null) {
      spanContext = spanContextualizer.link(thread.getJavaThreadId()).getSpanContext();
    }

    allocationEventExporter.export(event, sampler, spanContext);
  }

  public void flush() {
    allocationEventExporter.flush();
  }

  static Builder builder(Config config) {
    boolean enabled = Configuration.getTLABEnabled(config);
    Builder builder = new Builder(enabled);
    int samplerInterval = Configuration.getMemorySamplerInterval(config);
    if (samplerInterval > 1) {
      builder.sampler(new SystematicAllocationEventSampler(samplerInterval));
    }
    return builder;
  }

  static class Builder {
    private final boolean enabled;
    private AllocationEventExporter allocationEventExporter;
    private AllocationEventSampler sampler;
    private SpanContextualizer spanContextualizer;
    private StackTraceFilter stackTraceFilter;

    public Builder(boolean enabled) {
      this.enabled = enabled;
    }

    TLABProcessor build() {
      return new TLABProcessor(this);
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
