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

import static com.splunk.opentelemetry.profiler.LogExporterBuilder.INSTRUMENTATION_LIBRARY_INFO;

import com.splunk.opentelemetry.profiler.allocationsampler.AllocationEventSampler;
import com.splunk.opentelemetry.profiler.allocationsampler.SystematicAllocationEventSampler;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

public class TLABProcessor implements Consumer<RecordedEvent> {
  public static final String NEW_TLAB_EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  public static final String OUTSIDE_TLAB_EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  static final AttributeKey<Long> ALLOCATION_SIZE_KEY = AttributeKey.longKey("memory.allocated");

  private final boolean enabled;
  private final StackSerializer stackSerializer;
  private final LogProcessor logProcessor;
  private final LogDataCommonAttributes commonAttributes;
  private final Resource resource;
  private final AllocationEventSampler sampler;
  private final SpanContextualizer spanContextualizer;

  private TLABProcessor(Builder builder) {
    this.enabled = builder.enabled;
    this.stackSerializer = builder.stackSerializer;
    this.logProcessor = builder.logProcessor;
    this.commonAttributes = builder.commonAttributes;
    this.resource = builder.resource;
    this.sampler = builder.sampler;
    this.spanContextualizer = builder.spanContextualizer;
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
    // Discard events not chosen by the sampling strategy
    if (sampler != null && !sampler.shouldSample(event)) {
      return;
    }

    Instant time = event.getStartTime();

    String body = buildBody(event, stackTrace);

    AttributesBuilder builder =
        commonAttributes
            .builder(event.getEventType().getName())
            .put(ALLOCATION_SIZE_KEY, event.getLong("allocationSize"));
    if (sampler != null) {
      sampler.addAttributes(builder);
    }
    Attributes attributes = builder.build();

    LogDataBuilder logDataBuilder =
        LogDataBuilder.create(resource, INSTRUMENTATION_LIBRARY_INFO)
            .setEpoch(time)
            .setBody(body)
            .setAttributes(attributes);

    RecordedThread thread = event.getThread();
    if (thread != null) {
      SpanContext spanContext = spanContextualizer.link(thread.getJavaThreadId()).getSpanContext();
      if (spanContext.isValid()) {
        logDataBuilder.setSpanContext(spanContext);
      }
    }

    logProcessor.emit(logDataBuilder.build());
  }

  private String buildBody(RecordedEvent event, RecordedStackTrace stackTrace) {
    String stack = stackSerializer.serialize(stackTrace);
    RecordedThread thread = event.getThread();
    String name = thread == null ? "unknown" : thread.getJavaName();
    long id = thread == null ? 0 : thread.getJavaThreadId();
    return "\"" + name + "\"" + " #" + id + "\n" + "   java.lang.Thread.State: UNKNOWN\n" + stack;
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
    private StackSerializer stackSerializer = new StackSerializer();
    private LogProcessor logProcessor;
    private LogDataCommonAttributes commonAttributes;
    private Resource resource;
    private AllocationEventSampler sampler;
    private SpanContextualizer spanContextualizer;

    public Builder(boolean enabled) {
      this.enabled = enabled;
    }

    TLABProcessor build() {
      return new TLABProcessor(this);
    }

    Builder stackSerializer(StackSerializer stackSerializer) {
      this.stackSerializer = stackSerializer;
      return this;
    }

    Builder logProcessor(LogProcessor logsProcessor) {
      this.logProcessor = logsProcessor;
      return this;
    }

    Builder commonAttributes(LogDataCommonAttributes commonAttributes) {
      this.commonAttributes = commonAttributes;
      return this;
    }

    Builder resource(Resource resource) {
      this.resource = resource;
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
  }
}
