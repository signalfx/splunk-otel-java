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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JfrDirCleanupTest {

  @Test
  void testLifecycle() {
    Path path1 = Paths.get("/some/path/to/file1.jfr");
    Path path2 = Paths.get("/some/path/to/file1.jfr");
    Path path3 = Paths.get("/some/path/to/file1.jfr");

    Consumer<Path> fileDeleter = mock(Consumer.class);
    Runtime runtime = mock(Runtime.class);
    ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
    doNothing().when(runtime).addShutdownHook(threadCaptor.capture());

    JfrDirCleanup jfrDirCleanup =
        new JfrDirCleanup(fileDeleter) {
          @Override
          protected Runtime getRuntime() {
            return runtime;
          }
        };
    jfrDirCleanup.registerShutdownHook();

    jfrDirCleanup.recordingCreated(path1);
    jfrDirCleanup.recordingCreated(path2);
    jfrDirCleanup.recordingDeleted(path1);
    jfrDirCleanup.recordingCreated(path3);

    threadCaptor.getValue().run(); // not start, just runs inline

    verify(fileDeleter).accept(path2);
    verify(fileDeleter).accept(path3);
    verifyNoMoreInteractions(fileDeleter);
  }
}
