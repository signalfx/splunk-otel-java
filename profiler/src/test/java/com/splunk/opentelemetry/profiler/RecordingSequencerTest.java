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
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordingSequencerTest {

  Duration duration = Duration.ofMillis(10);

  @Mock JfrRecorder recorder;
  @Mock RecordingFileNamingConvention namingConvention;

  @Test
  void canContinueNotStarted() {
    when(recorder.isStarted()).thenReturn(false);
    RecordingSequencer sequencer = buildSequencer();
    sequencer.handleInterval();
    verify(recorder).start();
    verifyNoMoreInteractions(recorder);
  }

  @Test
  void canContinueAlreadyStarted() {
    when(recorder.isStarted()).thenReturn(true);
    RecordingSequencer sequencer = buildSequencer();
    sequencer.handleInterval();
    verify(recorder).flushSnapshot();
    verifyNoMoreInteractions(recorder);
  }

  @Test
  void startThroughFlushSequence() throws Exception {
    CountDownLatch latch = new CountDownLatch(3);
    recorder = new MockRecorder(latch);
    RecordingSequencer sequencer = buildSequencer();
    sequencer.start();
    assertTrue(latch.await(5, SECONDS));
  }

  private RecordingSequencer buildSequencer() {
    return buildSequencer(recorder);
  }

  private RecordingSequencer buildSequencer(JfrRecorder recorder) {
    return RecordingSequencer.builder().recordingDuration(duration).recorder(recorder).build();
  }

  private class MockRecorder extends JfrRecorder {
    private final CountDownLatch flushLatch;
    final CountDownLatch stopLatch = new CountDownLatch(1);
    boolean started;

    public MockRecorder(CountDownLatch flushLatch) {
      super(
          new Builder()
              .settings(Collections.emptyMap())
              .maxAgeDuration(Duration.ofSeconds(10))
              .namingConvention(namingConvention)
              .onNewRecording(mock(Consumer.class)));
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
  }
}
