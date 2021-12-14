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

import static java.util.Collections.emptyList;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OtlpLogsExporterTest {

  @RegisterExtension LogCapturer log = LogCapturer.create().captureForType(OtlpLogsExporter.class);

  @Test
  void exportLogs() {
    OtlpLogsExporter.ResponseHandler handler = new OtlpLogsExporter.ResponseHandler(emptyList());
    handler.onFailure(new RuntimeException("kaboom"));
    log.assertContains("failed to export logs");
  }
}
