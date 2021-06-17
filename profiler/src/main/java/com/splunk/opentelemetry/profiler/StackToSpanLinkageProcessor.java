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

import com.splunk.opentelemetry.logs.BatchingLogsProcessor;
import com.splunk.opentelemetry.logs.LogEntry;
import com.splunk.opentelemetry.logs.LogsProcessor;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import java.util.function.Consumer;

/**
 * Glue helper to turn a linked stack into an otel LogEntry instance and pass it to the logs
 * processor.
 */
public class StackToSpanLinkageProcessor implements Consumer<StackToSpanLinkage> {
  private final LogEntryCreator logEntryCreator;
  private final LogsProcessor processor;

  public StackToSpanLinkageProcessor(
      LogEntryCreator logEntryCreator, LogsProcessor processor) {
    this.logEntryCreator = logEntryCreator;
    this.processor = processor;
  }

  @Override
  public void accept(StackToSpanLinkage stackToSpanLinkage) {
    LogEntry log = logEntryCreator.apply(stackToSpanLinkage);
    processor.log(log);
  }
}
