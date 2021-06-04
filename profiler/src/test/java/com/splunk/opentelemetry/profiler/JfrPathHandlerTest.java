package com.splunk.opentelemetry.profiler;

import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JfrPathHandlerTest {

    @Test
    void testAccept() {
        Path path = Paths.get("/path/to/something.jfr");

        EventProcessingChain chain = mock(EventProcessingChain.class);
        Consumer<Path> onFileFinished = mock(Consumer.class);
        RecordedEventStream.Factory eventStreamFactory = mock(RecordedEventStream.Factory.class);
        RecordedEventStream recordedEventStream = mock(RecordedEventStream.class);
        RecordedEvent e1 = mock(RecordedEvent.class);
        RecordedEvent e2 = mock(RecordedEvent.class);
        RecordedEvent e3 = mock(RecordedEvent.class);

        when(eventStreamFactory.get()).thenReturn(recordedEventStream);
        when(recordedEventStream.open(path)).thenReturn(Stream.of(e1, e2, e3));

        JfrPathHandler jfrPathHandler = JfrPathHandler.builder()
                .eventProcessingChain(chain)
                .onFileFinished(onFileFinished)
                .recordedEventStreamFactory(eventStreamFactory)
                .build();

        jfrPathHandler.accept(path);

        verify(chain).accept(path, e1);
        verify(chain).accept(path, e2);
        verify(chain).accept(path, e3);
        verifyNoMoreInteractions(chain);
        verify(onFileFinished).accept(path);

    }

}