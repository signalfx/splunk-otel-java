package com.splunk.opentelemetry.profiler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstrumentationSourceTest {
  @ParameterizedTest
  @EnumSource(InstrumentationSource.class)
  void valueIsLowerCaseOfEnumName(InstrumentationSource source) {
    var value = source.value();
    assertEquals(source.name().toLowerCase(Locale.ROOT), value);
  }
}
