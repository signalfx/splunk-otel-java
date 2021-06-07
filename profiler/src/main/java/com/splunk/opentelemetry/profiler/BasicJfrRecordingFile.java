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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/** Simple/basic abstraction around a recording file. Can open and get a stream of events. */
class BasicJfrRecordingFile implements RecordedEventStream {

  private final JFR jfr;

  public BasicJfrRecordingFile(JFR jfr) {
    this.jfr = jfr;
  }

  @Override
  public Stream<RecordedEvent> open(Path path) {
    RecordingFile file = jfr.openRecordingFile(path);
    return StreamSupport.stream(
        new Spliterators.AbstractSpliterator<RecordedEvent>(Long.MAX_VALUE, Spliterator.ORDERED) {
          public boolean tryAdvance(Consumer<? super RecordedEvent> action) {
            if (file.hasMoreEvents()) {
              action.accept(jfr.readEvent(file, path));
              return true;
            }
            return false;
          }

          public void forEachRemaining(Consumer<? super RecordedEvent> action) {
            while (file.hasMoreEvents()) {
              action.accept(jfr.readEvent(file, path));
            }
          }
        },
        false);
  }
}
