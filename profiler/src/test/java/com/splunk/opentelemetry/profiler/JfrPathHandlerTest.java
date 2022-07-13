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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class JfrPathHandlerTest {

  @Test
  void testAccept() {
    Path path = Paths.get("/path/to/something.jfr");

    EventProcessingChain chain = mock(EventProcessingChain.class);
    @SuppressWarnings("unchecked")
    Consumer<Path> onFileFinished = mock(Consumer.class);
    RecordedEventStream.Factory eventStreamFactory = mock(RecordedEventStream.Factory.class);
    RecordedEventStream recordedEventStream = mock(RecordedEventStream.class);
    RecordedEvent e1 = mock(RecordedEvent.class);
    RecordedEvent e2 = mock(RecordedEvent.class);
    RecordedEvent e3 = mock(RecordedEvent.class);
    AtomicBoolean closeWasCalled = new AtomicBoolean(false);
    Stream<RecordedEvent> stream = Stream.of(e1, e2, e3).onClose(() -> closeWasCalled.set(true));

    when(eventStreamFactory.get()).thenReturn(recordedEventStream);
    when(recordedEventStream.open(path)).thenReturn(stream);

    JfrPathHandler jfrPathHandler =
        JfrPathHandler.builder()
            .eventProcessingChain(chain)
            .onFileFinished(onFileFinished)
            .recordedEventStreamFactory(eventStreamFactory)
            .build();

    jfrPathHandler.accept(path);

    verify(chain).accept(e1);
    verify(chain).accept(e2);
    verify(chain).accept(e3);
    verify(chain).flushBuffer();
    verify(chain).logEventStats();
    verifyNoMoreInteractions(chain);
    verify(onFileFinished).accept(path);
    assertTrue(closeWasCalled.get());
  }
}
