package com.splunk.opentelemetry.instrumentation.nocode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YamlParserTest {
  @Test
  void testBasicRuleParsesOK() {
    String yaml =
        "- class: someClass\n" +
            "  method: someMethod\n";
    assertEquals(1, YamlParser.parseFromString(yaml).size());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "class: fooButNoWrappingYamlList\n" +
          "  method: thisWillNotWork",
      "- class:\n" +
          "    named: cannotUseMultipleNameClauses\n"+
          "    nameMatches: withoutAnAndOrOr.*\n" +
          "  method: someMethodName",
      "- class:\n" +
          "    hasParameterCount: 1\n" +
          "  method: someMethodName\n",
      "- class:\n" +
          "    hasParameterOfType: 0 int\n" +
          "  method: someMethodName\n",
      "- class:\n" +
          "    and:\n" +
          "  method: someMethodName\n",
      "- class:\n" +
          "    or:\n" +
          "      not:\n" +
          "        named: singleRuleExpected\n" +
          "        nameMatches: underANot.8\n" +
          "  method: someMethodName\n",
      "- class:\n" +
          "    or:\n" +
          "      not: bareValueOnlySupportedForSimpleClassNames\n" +
          "  method: someMethodName\n",
      "- class: someClassName\n" +
          "  method:\n" +
          "    hasSuperType: notExpectedForMethodMatcher\n",
      "- class: someClassName\n" +
          "  method:\n" +
          "    hasParameterCount: notanumber\n",
      "- class: someClassName\n" +
          "  method:\n" +
          "    hasParameterOfType: onlyOneArgument\n",
      "- class: someClassName\n" +
          "  method:\n" +
          "    hasParameterOfType: notanumber int\n",
  })
  void invalidYamlIsInvalid(String yaml) {
    assertEquals(0, YamlParser.parseFromString(yaml).size());
  }
}
