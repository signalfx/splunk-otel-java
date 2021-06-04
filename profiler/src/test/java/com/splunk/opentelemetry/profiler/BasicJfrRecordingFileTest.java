package com.splunk.opentelemetry.profiler;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicJfrRecordingFileTest {

    @Mock
    JFR jfr;
    @Mock
    RecordingFile recordingFile;
    Path path = Paths.get("/path/to/file.jfr");

    @Test
    void testOpen() {
        RecordedEvent e1 = mock(RecordedEvent.class);
        RecordedEvent e2 = mock(RecordedEvent.class);
        RecordedEvent e3 = mock(RecordedEvent.class);

        when(recordingFile.hasMoreEvents()).thenReturn(true, true, true, false);
        when(jfr.openRecordingFile(path)).thenReturn(recordingFile);
        when(jfr.readEvent(recordingFile, path)).thenReturn(e1, e2, e3);
        BasicJfrRecordingFile recordingFile = new BasicJfrRecordingFile(jfr);

        Stream<RecordedEvent> stream = recordingFile.open(path);
        List<RecordedEvent> result = stream.collect(Collectors.toList());
        List<RecordedEvent> expected = Arrays.asList(e1, e2, e3);
        assertEquals(expected, result);
    }

}