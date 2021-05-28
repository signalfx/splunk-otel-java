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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordingSequencerTest {

    Duration duration = Duration.ofMillis(10);

    @Mock
    JfrRecorder recorder;
    @Mock
    RecordingEscapeHatch escapeHatch;

    @Test
    void canContinueNotStarted() {
        when(escapeHatch.jfrCanContinue()).thenReturn(true);
        when(recorder.isStarted()).thenReturn(false);
        RecordingSequencer sequencer = buildSequencer();
        sequencer.handleInterval();
        verify(recorder).start();
        verifyNoMoreInteractions(recorder);
  }

    @Test
    void canContinueAlreadyStarted() {
        when(escapeHatch.jfrCanContinue()).thenReturn(true);
    when(recorder.isStarted()).thenReturn(true);
        RecordingSequencer sequencer = buildSequencer();
        sequencer.handleInterval();
        verify(recorder).flushSnapshot();
        verifyNoMoreInteractions(recorder);
    }

    @Test
    void startThroughFlushSequence() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
    when(escapeHatch.jfrCanContinue()).thenReturn(true);
    recorder = new MockRecorder(latch);
        RecordingSequencer sequencer = buildSequencer();
        sequencer.start();
        assertTrue(latch.await(5, SECONDS));
    }

    @Test
    void testUnhealthyNotStarted() {
        when(escapeHatch.jfrCanContinue()).thenReturn(false);
        when(recorder.isStarted()).thenReturn(false);
        RecordingSequencer sequencer = buildSequencer();
        sequencer.handleInterval();
    verify(recorder, never()).start();
    verify(recorder, never()).flushSnapshot();
    }

    @Test
    void testUnhealthyStartedCallsStop() {
        when(escapeHatch.jfrCanContinue()).thenReturn(true);
        when(recorder.isStarted()).thenReturn(false);
        RecordingSequencer sequencer = buildSequencer();

        // First time through we start
        sequencer.handleInterval();
        verify(recorder).start();
        verify(recorder, never()).flushSnapshot();
        when(recorder.isStarted()).thenReturn(true);

        // Second time through we flush
        sequencer.handleInterval();
        verify(recorder).flushSnapshot();
        verify(recorder, never()).stop();

        // Now we are broken, so we call stop()
        when(escapeHatch.jfrCanContinue()).thenReturn(false);
        sequencer.handleInterval();
        verify(recorder).stop();
        verify(recorder, times(1)).flushSnapshot();
        verify(recorder, times(1)).start();
    }

    @Test
    void testRecoversAfterSkipping() throws Exception {
        CountDownLatch flushLatch = new CountDownLatch(3);
        when(escapeHatch.jfrCanContinue()).thenReturn(true);
        MockRecorder recorder = new MockRecorder(flushLatch);
        RecordingSequencer sequencer = buildSequencer(recorder);
    // First time through we start
    sequencer.handleInterval();
    verify(recorder).start();
    verify(recorder, never()).flushSnapshot();
    when(recorder.isStarted()).thenReturn(true);

    // Second time through we flush
    sequencer.handleInterval();
    verify(recorder).flushSnapshot();
    verify(recorder, never()).stop();

    // Now we are broken, so we call stop()
    when(escapeHatch.jfrCanContinue()).thenReturn(false);
    sequencer.handleInterval();
    verify(recorder).stop();
    verify(recorder, times(1)).flushSnapshot();
    verify(recorder, times(1)).start();
  }

  @Test
  void testRecoversAfterSkipping() throws Exception {
    CountDownLatch flushLatch = new CountDownLatch(3);
    when(escapeHatch.jfrCanContinue()).thenReturn(true);
    MockRecorder recorder = new MockRecorder(flushLatch);
    RecordingSequencer sequencer = buildSequencer(recorder);    sequencer.start();
        assertTrue(flushLatch.await(5, SECONDS));
        assertEquals(1, recorder.stopLatch.getCount());
        assertTrue(recorder.isStarted());

        // After 3 flushes, we are now broken!
        when(escapeHatch.jfrCanContinue()).thenReturn(false);
        assertTrue(recorder.stopLatch.await(5, SECONDS));
        assertFalse(recorder.isStarted());

        flushLatch = new CountDownLatch(3);
        recorder.setFlushLatch(flushLatch);
        // And now we got happy again
        when(escapeHatch.jfrCanContinue()).thenReturn(true);
        assertTrue(flushLatch.await(5, SECONDS));
        assertTrue(recorder.isStarted());


    }

  private RecordingSequencer buildSequencer() {
    return buildSequencer(recorder);
  }

  private RecordingSequencer buildSequencer(JfrRecorder recorder) {
    return RecordingSequencer.builder()
        .recordingEscapeHatch(escapeHatch)
        .recordingDuration(duration)
        .recorder(recorder)
        .build();
  }

  private static class MockRecorder extends JfrRecorder {
    private volatile CountDownLatch flushLatch;
    final CountDownLatch stopLatch = new CountDownLatch(1);
    boolean started;

    public MockRecorder(CountDownLatch flushLatch) {
      this.flushLatch = flushLatch;
      started = false;
    }

    @Override
    public void start() {
      if (started) {
        fail("Already started");
      }
      started = true;
    }

    @Override
    public void flushSnapshot() {
      assertTrue(started);
      flushLatch.countDown();
    }

    @Override
    public boolean isStarted() {
      return started;
    }

    @Override
    public void stop() {
      if (!started) {
        fail("not started");
      }
      started = false;
      stopLatch.countDown();
    }

    public void setFlushLatch(CountDownLatch flushLatch) {
      this.flushLatch = flushLatch;
    }
  }
}
