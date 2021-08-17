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

import com.splunk.opentelemetry.logs.LogEntry;
import com.splunk.opentelemetry.logs.LogsProcessor;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;

import java.time.Instant;
import java.util.function.Consumer;

import static com.splunk.opentelemetry.profiler.LogEntryCreator.PROFILING_SOURCE;

public class TLABProcessor implements Consumer<RecordedEvent> {
  public static final String NEW_TLAB_EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";
  public static final String OUTSIDE_TLAB_EVENT_NAME = "jdk.ObjectAllocationOutsideTLAB";
  final static AttributeKey<Long> ALLOCATION_SIZE_KEY = AttributeKey.longKey("allocationSize");
  final static AttributeKey<Long> TLAB_SIZE_KEY = AttributeKey.longKey("tlabSize");

  private final StackSerializer stackSerializer;
  private final LogsProcessor batchingLogsProcessor;
  private final LogEntryCommonAttributes commonAttributes;

  public TLABProcessor(LogsProcessor batchingLogsProcessor, LogEntryCommonAttributes commonAttributes) {
    this(new StackSerializer(), batchingLogsProcessor, commonAttributes);
  }

  public TLABProcessor(StackSerializer stackSerializer, LogsProcessor batchingLogsProcessor, LogEntryCommonAttributes commonAttributes) {
    this.stackSerializer = stackSerializer;
    this.batchingLogsProcessor = batchingLogsProcessor;
    this.commonAttributes = commonAttributes;
  }

  @Override
  public void accept(RecordedEvent event) {
    RecordedStackTrace stackTrace = event.getStackTrace();
    if(stackTrace == null){
      return;
    }
    Instant time = event.getStartTime();
    String stack = stackSerializer.serialize(stackTrace);
    AttributesBuilder builder = commonAttributes.build(event)
            .toBuilder()
            .put(ALLOCATION_SIZE_KEY, event.getLong("allocationSize"));

    if(event.hasField("tlabSize")){
      builder.put(TLAB_SIZE_KEY, event.getLong("tlabSize"));
    }
    Attributes attributes = builder.build();

    LogEntry logEntry = LogEntry.builder()
            .name(PROFILING_SOURCE)
            .time(time)
            .body(stack)
            .attributes(attributes)
            .build();

    batchingLogsProcessor.log(logEntry);
  }


}
