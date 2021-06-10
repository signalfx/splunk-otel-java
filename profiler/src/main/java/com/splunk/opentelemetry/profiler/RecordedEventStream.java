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
import java.util.function.Supplier;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/** Tag interface for turning a file path into a stream of JFR RecordedEvent instances. */
interface RecordedEventStream {

  /**
   * Opens a path to a jfr recording file and turns it into a stream of RecordedEvents from that
   * file. It is the callers responsibility to call close() on the stream when finished, and failing
   * to do so might result in resources being leaked or held open.
   *
   * @param path - path to a jfr recording file
   * @return Stream of all RecordedEvents from the given file
   */
  Stream<RecordedEvent> open(Path path);

  interface Factory extends Supplier<RecordedEventStream> {}
}
