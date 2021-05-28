package com.splunk.opentelemetry.profiler;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.splunk.opentelemetry.profiler.JfrRecorder.RECORDING_NAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JfrRecorderTest {

    Duration maxAge = Duration.ofMinutes(13);
    Map<String, String> settings;
    @Mock
    JfrSettingsReader settingsReader;
    @Mock
    Recording recording;

    @BeforeEach
    void setup() {
        settings = new HashMap<>();
        settings.put("foo", "bar");
    }

    @Test
    void testStart() {
        when(settingsReader.read()).thenReturn(settings);
        JfrRecorder jfrRecorder = buildJfrRecorder(mock(JFR.class));
        jfrRecorder.start();
        verify(recording).setSettings(settings);
        verify(recording).setToDisk(false);
        verify(recording).setName(RECORDING_NAME);
        verify(recording).setDuration(null);
        verify(recording).setMaxAge(maxAge);
        verify(recording).start();
    }

    @Test
    void testFlushSnapshot() throws Exception {
        JFR jfr = mock(JFR.class);
        Recording snap = mock(Recording.class);
        when(jfr.takeSnapshot()).thenReturn(snap);
        JfrRecorder jfrRecorder = buildJfrRecorder(jfr);
        jfrRecorder.flushSnapshot();
        verify(snap).dump(isA(Path.class));
        verify(snap).close();
    }

    @Test
    void testDumpThrows() throws Exception {
        JFR jfr = mock(JFR.class);
        JfrRecorder jfrRecorder = buildJfrRecorder(jfr);
        Recording snap = mock(Recording.class);
        when(jfr.takeSnapshot()).thenReturn(snap);
        doThrow(new IOException("KABOOM!!!!!!!!")).when(snap).dump(isA(Path.class));
        jfrRecorder.flushSnapshot();
        // No exception propagated
    }

    @Test
    void testIsStarted() {
        JFR jfr = mock(JFR.class);
        JfrRecorder jfrRecorder = buildJfrRecorder(jfr);
        assertFalse(jfrRecorder.isStarted());
        jfrRecorder.start();
        when(recording.getState()).thenReturn(RecordingState.RUNNING);
        assertTrue(jfrRecorder.isStarted());
    }

    @Test
    void testIsStop() {
        JFR jfr = mock(JFR.class);
        JfrRecorder jfrRecorder = buildJfrRecorder(jfr);
        assertFalse(jfrRecorder.isStarted());
        jfrRecorder.start();
        verify(recording, never()).stop();
        jfrRecorder.stop();
        verify(recording).stop();
        assertFalse(jfrRecorder.isStarted());
    }

    private JfrRecorder buildJfrRecorder(JFR jfr) {
        JfrRecorder.Builder builder = JfrRecorder.builder()
                .maxAgeDuration(maxAge)
                .settingsReader(settingsReader)
                .jfr(jfr);

        return new JfrRecorder(builder) {
            @Override
            Recording newRecording() {
                return recording;
            }
        };
    }

}