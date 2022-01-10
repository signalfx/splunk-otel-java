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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.logs.BatchingLogsProcessor;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StackToSpanLinkageProcessorTest {

  @Test
  void testProcessor() {
    Instant time = Instant.now();
    StackToSpanLinkage linkedSpan =
        new StackToSpanLinkage(time, "some stack", "event name", SpanLinkage.NONE);
    LogDataCreator logCreator = mock(LogDataCreator.class);
    BatchingLogsProcessor exportProcessor = mock(BatchingLogsProcessor.class);

    LogDataBuilder builder =
        LogDataBuilder.create(
            Resource.getDefault(), InstrumentationLibraryInfo.create("test", "1.2.3"));

    LogData log = builder.setBody("the.body").build();
    when(logCreator.apply(linkedSpan)).thenReturn(log);

    StackToSpanLinkageProcessor processor =
        new StackToSpanLinkageProcessor(logCreator, exportProcessor);

    processor.accept(linkedSpan);
    verify(exportProcessor).emit(log);
  }
}
