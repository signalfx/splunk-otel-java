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

package com.splunk.opentelemetry.instrumentation.nocode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YamlParserTest {
  @Test
  void testBasicRuleParsesOK() {
    String yaml = "- class: someClass\n" + "  method: someMethod\n";
    assertEquals(1, YamlParser.parseFromString(yaml).size());
  }

  // formatting of the strings with newlines makes it easier to read and
  // reason about the test.
  // spotless:off
  @ParameterizedTest
  @ValueSource(
      strings = {
        "class: fooButNoWrappingYamlList\n"
            + "  method: thisWillNotWork",
        "- class:\n"
            + "    - name: cannotUseMultipleNameClauses\n"
            + "    - nameRegex: withoutAnAndOrOr.*\n"
            + "  method: someMethodName",
        "- class:\n"
            + "    parameterCount: 1\n"
            + "    method: someMethodName\n",
        "- class:\n"
            + "    parameter:\n"
            + "      index: 0\n"
            + "      type: int\n"
            + "  method: someMethodName\n",
        "- class:\n"
            + "    and:\n"
            + "  method: someMethodName\n", // no clauses in and:
        "- class:\n"
            + "    or:\n"
            + "      - not:\n"
            + "          name: singleRuleExpected\n"
            + "          nameRegex: underANot.*\n"
            + "  method: someMethodName\n",
        "- class:\n"
            + "    or:\n"
            + "      - not: bareValueOnlySupportedForSimpleClassNames\n"
            + "  method: someMethodName\n",
        "- class: someClassName\n"
            + "  method:\n"
            + "    superType: notExpectedForMethodMatcher\n",
        "- class: someClassName\n"
            + "  method:\n"
            + "    parameterCount: notanumber\n",
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter:\n"
              + "      index: notanumber\n"
              + "      type: int\n",
        "- class: someClassName\n"
            + "  method:\n"
            + "    parameter:\n"
            + "      index: 0\n", // no type
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter:\n"
              + "      type: int\n", // no index
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter: bareValueNotSupported\n",
          "- class: someClassName\n"
              + "  method:\n"
              + "    parameter:\n"
              + "      noIndex: 1\n"
              + "      noType: int\n",
      })
  // spotless:on
  void invalidYamlIsInvalid(String yaml) {
    assertEquals(0, YamlParser.parseFromString(yaml).size());
  }
}
