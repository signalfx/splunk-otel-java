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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordingEscapeHatchTest {

  public static final long PLENTY_OF_SPACE = 250 * 1024 * 1024L;
  public static final long NOT_ENOUGH_SPACE = 29L;
  Duration recordingDuration = Duration.ofSeconds(15);
  Path outputPath = Paths.get("/some/output/path");
  RecordingEscapeHatch.FilesShim filesShim;
  RecordingFileNamingConvention convention;
  FileStore fileStore;

  @BeforeEach
  void setup() throws Exception {
    filesShim = mock(RecordingEscapeHatch.FilesShim.class);
    convention = mock(RecordingFileNamingConvention.class);
    fileStore = mock(FileStore.class);
    when(convention.getOutputPath()).thenReturn(outputPath);
    when(filesShim.getFileStore(outputPath)).thenReturn(fileStore);
    when(filesShim.isRegularFile(isA(Path.class))).thenReturn(true);
    doAnswer(
            invocation -> {
              Path path = invocation.getArgument(0);
              return path.toString().matches("/path/to/file\\d+.jfr");
            })
        .when(convention)
        .matches(isA(Path.class));
  }

  @Test
  void testCanContinueHappyPath() throws Exception {
    when(fileStore.getUsableSpace()).thenReturn(PLENTY_OF_SPACE);
    Stream<Path> outputFiles = makeFiles(5);
    when(filesShim.list(outputPath)).thenReturn(outputFiles);
    RecordingEscapeHatch escapeHatch = buildEscapeHatch(false);
    assertTrue(escapeHatch.jfrCanContinue());
  }

  @Test
  void testTooManyFilesOnDisk() throws Exception {
    when(fileStore.getUsableSpace()).thenReturn(PLENTY_OF_SPACE);
    Stream<Path> outputFiles = makeFiles(5000);
    when(filesShim.list(outputPath)).thenReturn(outputFiles);
    boolean keepFiles = false;
    RecordingEscapeHatch escapeHatch = buildEscapeHatch(keepFiles);
    assertFalse(escapeHatch.jfrCanContinue());
  }

  @Test
  void testTooManyFilesOnDiskButKeepFilesSpecified() throws Exception {
    when(fileStore.getUsableSpace()).thenReturn(PLENTY_OF_SPACE);
    Stream<Path> outputFiles = makeFiles(5000);
    when(filesShim.list(outputPath)).thenReturn(outputFiles);
    RecordingEscapeHatch escapeHatch = buildEscapeHatch(true);
    assertTrue(escapeHatch.jfrCanContinue());
  }

  @Test
  void testNotEnoughFreeSpace() throws Exception {
    when(fileStore.getUsableSpace()).thenReturn(NOT_ENOUGH_SPACE);
    Stream<Path> outputFiles = makeFiles(5);
    when(filesShim.list(outputPath)).thenReturn(outputFiles);
    RecordingEscapeHatch escapeHatch = buildEscapeHatch(false);
    assertFalse(escapeHatch.jfrCanContinue());
  }

  @Test
  void testFileStoreException() throws Exception {
    when(fileStore.getUsableSpace()).thenReturn(PLENTY_OF_SPACE);
    when(filesShim.list(outputPath)).thenThrow(new IOException("KABOOM"));
    RecordingEscapeHatch escapeHatch = buildEscapeHatch(false);
    assertFalse(escapeHatch.jfrCanContinue());
  }

  private RecordingEscapeHatch buildEscapeHatch(boolean keepFiles) {
    return RecordingEscapeHatch.builder()
        .recordingDuration(recordingDuration)
        .configKeepsFilesOnDisk(keepFiles)
        .namingConvention(convention)
        .filesShim(filesShim)
        .build();
  }

  private Stream<Path> makeFiles(int num) {
    return IntStream.range(0, num).mapToObj(i -> Paths.get("/path/to/file" + i + ".jfr"));
  }
}
