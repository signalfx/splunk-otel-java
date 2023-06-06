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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingFileNamingConventionTest {

  @TempDir Path outputDir;

  @Test
  void testNewPath() throws IOException {
    RecordingFileNamingConvention convention = new RecordingFileNamingConvention(outputDir);
    LocalDateTime now = LocalDateTime.of(1999, Month.FEBRUARY, 12, 17, 3, 21);
    Path expected = outputDir.resolve("otel-profiler-1999-02-12T17_03_21");

    Path path = convention.newOutputPath(now);

    assertThat(path.toString()).startsWith(expected.toString()).endsWith(".jfr");
  }
}
