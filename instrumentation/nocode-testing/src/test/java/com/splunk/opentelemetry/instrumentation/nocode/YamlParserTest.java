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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "class: fooButNoWrappingYamlList\n" + "  method: thisWillNotWork",
        "- class:\n"
            + "    - named: cannotUseMultipleNameClauses\n"
            + "    - nameMatches: withoutAnAndOrOr.*\n"
            + "  method: someMethodName",
        "- class:\n" + "    hasParameterCount: 1\n" + "  method: someMethodName\n",
        "- class:\n" + "    hasParameterOfType: 0 int\n" + "  method: someMethodName\n",
        "- class:\n" + "    and:\n" + "  method: someMethodName\n", // no clauses in and:
        "- class:\n"
            + "    or:\n"
            + "      - not:\n"
            + "          named: singleRuleExpected\n"
            + "          nameMatches: underANot.*\n"
            + "  method: someMethodName\n",
        "- class:\n"
            + "    or:\n"
            + "      - not: bareValueOnlySupportedForSimpleClassNames\n"
            + "  method: someMethodName\n",
        "- class: someClassName\n"
            + "  method:\n"
            + "    hasSuperType: notExpectedForMethodMatcher\n",
        "- class: someClassName\n" + "  method:\n" + "    hasParameterCount: notanumber\n",
        "- class: someClassName\n" + "  method:\n" + "    hasParameterOfType: onlyOneArgument\n",
        "- class: someClassName\n" + "  method:\n" + "    hasParameterOfType: notanumber int\n",
      })
  void invalidYamlIsInvalid(String yaml) {
    assertEquals(0, YamlParser.parseFromString(yaml).size());
  }
}
