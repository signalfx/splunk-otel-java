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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;
import static com.splunk.opentelemetry.profiler.LogEntryCreator.PROFILING_SOURCE;

import com.splunk.opentelemetry.logs.LogEntry;
import com.splunk.opentelemetry.logs.LogsProcessor;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.config.Config;
import java.time.Instant;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;

public class TLABProcessor implements Consumer<RecordedEvent> {
  public static final String NEW_TLAB_EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  public static final String OUTSIDE_TLAB_EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  static final AttributeKey<Long> ALLOCATION_SIZE_KEY = AttributeKey.longKey("allocationSize");
  static final AttributeKey<Long> TLAB_SIZE_KEY = AttributeKey.longKey("tlabSize");

  private final boolean enabled;
  private final StackSerializer stackSerializer;
  private final LogsProcessor batchingLogsProcessor;
  private final LogEntryCommonAttributes commonAttributes;

  public TLABProcessor(
      Config config,
      LogsProcessor batchingLogsProcessor,
      LogEntryCommonAttributes commonAttributes) {
    this(config, new StackSerializer(), batchingLogsProcessor, commonAttributes);
  }

  public TLABProcessor(
      Config config,
      StackSerializer stackSerializer,
      LogsProcessor batchingLogsProcessor,
      LogEntryCommonAttributes commonAttributes) {
    this.enabled = config.getBoolean(CONFIG_KEY_TLAB_ENABLED);
    this.stackSerializer = stackSerializer;
    this.batchingLogsProcessor = batchingLogsProcessor;
    this.commonAttributes = commonAttributes;
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

    if (event.hasField("tlabSize")) {
      builder.put(TLAB_SIZE_KEY, event.getLong("tlabSize"));
    }
    Attributes attributes = builder.build();

    LogEntry logEntry =
        LogEntry.builder()
            .name(PROFILING_SOURCE)
            .time(time)
            .bodyString(stack)
            .attributes(attributes)
            .build();

    batchingLogsProcessor.log(logEntry);
  }
}
