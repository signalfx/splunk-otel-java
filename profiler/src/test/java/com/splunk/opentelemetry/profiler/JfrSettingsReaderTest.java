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

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JfrSettingsReaderTest {

  @Test
  void testReader() {
    String content =
        "jdk.EvacuationFailed#enabled=true\n"
            + "# lines can start with comments\n"
            + "jdk.ClassLoad#threshold=0 ms\n"
            + "  # and comments can be indented\n"
            + "jdk.ReservedStackActivation#enabled=true\n";

    Map<String, String> expected = new HashMap<>();
    expected.put("jdk.EvacuationFailed#enabled", "true");
    expected.put("jdk.ClassLoad#threshold", "0 ms");
    expected.put("jdk.ReservedStackActivation#enabled", "true");

    BufferedReader reader = new BufferedReader(new StringReader(content));
    JfrSettingsReader settingsReader =
        new JfrSettingsReader() {
          @Override
          BufferedReader openResource(String resourceName) {
            return reader;
          }
        };
    Map<String, String> result = settingsReader.read();
    assertEquals(expected, result);
  }

  @Test
  void testCannotFindResource() {
    JfrSettingsReader settingsReader =
        new JfrSettingsReader() {
          @Override
          BufferedReader openResource(String resourceName) {
            return null;
          }
        };
    Map<String, String> result = settingsReader.read();
    assertTrue(result.isEmpty());
  }
}
