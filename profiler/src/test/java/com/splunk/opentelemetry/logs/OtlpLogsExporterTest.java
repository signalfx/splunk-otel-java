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

package com.splunk.opentelemetry.logs;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.netmikey.logunit.api.LogCapturer;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OtlpLogsExporterTest {

  @RegisterExtension LogCapturer log = LogCapturer.create().captureForType(OtlpLogsExporter.class);

  @Test
  void oncePerHourLogging_debugDisabled() {
    Clock clock = mock(Clock.class);
    Instant firstTime = Instant.now();
    Instant secondTime = firstTime.plus(45, MINUTES);
    Instant thirdTime = firstTime.plus(1, HOURS);
    when(clock.instant()).thenReturn(firstTime, secondTime, thirdTime);

    OtlpLogsExporter.OncePerHourLogger perHour = new OtlpLogsExporter.OncePerHourLogger(clock);
    perHour.log("test1", null);
    perHour.log("test2", null); // should skip
    perHour.log("test3", null); // should register

    log.assertContains("test1");
    log.assertDoesNotContain("test2");
    log.assertContains("test3");
  }
}
