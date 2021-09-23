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

import com.splunk.opentelemetry.profiler.events.RelevantEvents;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/**
 * Limits a delegate stream to just the events that we care about and returns them sorted by start
 * time.
 */
class FilterSortedRecordingFile implements RecordedEventStream {

  private final RecordedEventStream.Factory delegateStreamFactory;
  private final RelevantEvents relevantEvents;

  FilterSortedRecordingFile(Factory delegateStreamFactory, RelevantEvents relevantEvents) {
    this.delegateStreamFactory = delegateStreamFactory;
    this.relevantEvents = relevantEvents;
  }

  @Override
  public Stream<RecordedEvent> open(Path path) {
    return delegateStreamFactory
        .get()
        .open(path)
        .filter(relevantEvents::isRelevant)
        .sorted(Comparator.comparing(RecordedEvent::getStartTime));
  }

}
