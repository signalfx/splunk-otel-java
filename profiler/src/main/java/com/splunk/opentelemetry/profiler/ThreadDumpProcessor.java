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

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadDumpProcessor {
  public static final String EVENT_NAME = "jdk.ThreadDump";
  private static final Logger logger = LoggerFactory.getLogger(ThreadDumpProcessor.class);
  private final SpanContextualizer contextualizer;
  private final Consumer<StackToSpanLinkage> processor;
  private final ThreadDumpToStacks threadDumpToStacks;

  public ThreadDumpProcessor(Builder builder) {
    this.contextualizer = builder.contextualizer;
    this.processor = builder.processor;
    this.threadDumpToStacks = builder.threadDumpToStacks;
  }

  public void accept(RecordedEvent event) {
    String eventName = event.getEventType().getName();
    logger.debug("Processing JFR event {}", eventName);
    String wallOfStacks = event.getString("result");
    threadDumpToStacks
        .toStream(wallOfStacks)
        .map(stack -> contextualizer.link(event.getStartTime(), stack, event))
        .forEach(processor);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private SpanContextualizer contextualizer;
    private Consumer<StackToSpanLinkage> processor;
    private ThreadDumpToStacks threadDumpToStacks;

    public Builder spanContextualizer(SpanContextualizer contextualizer) {
      this.contextualizer = contextualizer;
      return this;
    }

    public Builder processor(Consumer<StackToSpanLinkage> consumer) {
      this.processor = consumer;
      return this;
    }

    public Builder threadDumpToStacks(ThreadDumpToStacks threadDumpToStacks) {
      this.threadDumpToStacks = threadDumpToStacks;
      return this;
    }

    public ThreadDumpProcessor build() {
      return new ThreadDumpProcessor(this);
    }
  }
}
