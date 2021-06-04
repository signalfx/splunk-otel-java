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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JfrFileLifecycleEventsTest {

  @Mock JfrDirCleanup cleanup;
  Path path = Paths.get("/path/to/some/file.jfr");

  @Test
  void testOnNewRecording() {
    Consumer<Path> pathHandler = mock(Consumer.class);
    Consumer<Path> callback = JfrFileLifecycleEvents.buildOnNewRecording(pathHandler, cleanup);
    callback.accept(path);
    verify(pathHandler).accept(path);
    verify(cleanup).recordingCreated(path);
  }

  @Test
  void testOnFileFinished() {
    Consumer<Path> deleter = mock(Consumer.class);
    Consumer<Path> callback = JfrFileLifecycleEvents.buildOnFileFinished(deleter, cleanup);
    callback.accept(path);
    verify(deleter).accept(path);
    verify(cleanup).recordingDeleted(path);
  }
}
