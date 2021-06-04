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

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for processing a single jfr file snapshot. It streams events from the
 * RecordedEventStream into the processing chain and, once complete, calls the onComplete callback.
 */
public class JfrPathHandler implements Consumer<Path> {

  private static final Logger logger = LoggerFactory.getLogger(JfrPathHandler.class);
  private final EventProcessingChain eventProcessingChain;
  private final Consumer<Path> onFileFinished;
  private final RecordedEventStream.Factory recordedEventStreamFactory;

  public JfrPathHandler(Builder builder) {
    this.eventProcessingChain = builder.eventProcessingChain;
    this.onFileFinished = builder.onFileFinished;
    this.recordedEventStreamFactory = builder.recordedEventStreamFactory;
  }

  @Override
  public void accept(Path path) {
    logger.info("New jfr file detected: " + path);
    RecordedEventStream recordingFile = recordedEventStreamFactory.get();
    Stream<RecordedEvent> events = recordingFile.open(path);
    events.forEach(event -> eventProcessingChain.accept(path, event));
    onFileFinished.accept(path);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private EventProcessingChain eventProcessingChain;
    private Consumer<Path> onFileFinished;
    private RecordedEventStream.Factory recordedEventStreamFactory;

    public Builder eventProcessingChain(EventProcessingChain eventProcessingChain) {
      this.eventProcessingChain = eventProcessingChain;
      return this;
    }

    public Builder onFileFinished(Consumer<Path> onFileFinished) {
      this.onFileFinished = onFileFinished;
      return this;
    }

    public Builder recordedEventStreamFactory(
        RecordedEventStream.Factory recordedEventStreamFactory) {
      this.recordedEventStreamFactory = recordedEventStreamFactory;
      return this;
    }

    public JfrPathHandler build() {
      return new JfrPathHandler(this);
    }
  }
}
