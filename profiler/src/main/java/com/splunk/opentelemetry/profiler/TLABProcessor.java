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
import static com.splunk.opentelemetry.profiler.LogsExporterBuilder.INSTRUMENTATION_LIBRARY_INFO;

import com.splunk.opentelemetry.logs.LogsProcessor;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;

public class TLABProcessor implements Consumer<RecordedEvent> {
  public static final String NEW_TLAB_EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  public static final String OUTSIDE_TLAB_EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  static final AttributeKey<Long> ALLOCATION_SIZE_KEY = AttributeKey.longKey("memory.allocated");

  private final boolean enabled;
  private final StackSerializer stackSerializer;
  private final LogsProcessor logsProcessor;
  private final LogDataCommonAttributes commonAttributes;
  private final Resource resource;

  private TLABProcessor(Builder builder) {
    this.enabled = builder.enabled;
    this.stackSerializer = builder.stackSerializer;
    this.logsProcessor = builder.logsProcessor;
    this.commonAttributes = builder.commonAttributes;
    this.resource = builder.resource;
  }

  @Override
  public void accept(RecordedEvent event) {
    // If there is another JFR recording in progress that has enabled TLAB events we might also get
    // them because JFR
    // sends all enabled events to all recordings, if that is the case ignore them.
    if (!enabled) {
      return;
    }
    RecordedStackTrace stackTrace = event.getStackTrace();
    if (stackTrace == null) {
      return;
    }
    Instant time = event.getStartTime();
    String stack = stackSerializer.serialize(stackTrace);
    AttributesBuilder builder =
        commonAttributes.build(event.getEventType().getName()).toBuilder()
            .put(ALLOCATION_SIZE_KEY, event.getLong("allocationSize"));
    Attributes attributes = builder.build();

    LogData logData =
        LogDataBuilder.create(resource, INSTRUMENTATION_LIBRARY_INFO)
            .setName(PROFILING_SOURCE)
            .setEpoch(time)
            .setBody(stack)
            .setAttributes(attributes)
            .build();

    logsProcessor.log(logData);
  }

  static Builder builder(Config config) {
    boolean enabled = Configuration.getTLABEnabled(config);
    return new Builder(enabled);
  }

  static class Builder {
    private boolean enabled;
    private StackSerializer stackSerializer = new StackSerializer();
    private LogsProcessor logsProcessor;
    private LogDataCommonAttributes commonAttributes;
    private Resource resource;

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

    Builder logsProcessor(LogsProcessor logsProcessor) {
      this.logsProcessor = logsProcessor;
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
  }
}
