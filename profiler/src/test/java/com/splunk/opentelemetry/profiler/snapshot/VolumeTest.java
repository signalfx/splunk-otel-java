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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class VolumeTest {
  @ParameterizedTest
  @EnumSource(Volume.class)
  void toStringRepresentation(Volume volume) {
    assertEquals(volume.name().toLowerCase(Locale.ROOT), volume.toString());
  }

  @ParameterizedTest
  @MethodSource("volumesAsStrings")
  void fromStringRepresentation(Volume volume, String asString) {
    assertEquals(volume, Volume.fromString(asString));
  }

  @ParameterizedTest
  @MethodSource("volumesAsStrings")
  void fromStringIsNotSensitiveToLocale(Volume volume, String asString) {
    var defaultLocale = Locale.getDefault();
    try {
      for (var locale : Locale.getAvailableLocales()) {
        Locale.setDefault(locale);
        assertEquals(volume, Volume.fromString(asString));
      }
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  private static Stream<Arguments> volumesAsStrings() {
    return Stream.of(
        Arguments.of(Volume.OFF, "off"),
        Arguments.of(Volume.HIGHEST, "highest"),
        Arguments.of(Volume.HIGHEST, "hıghest"));
  }

  @ParameterizedTest
  @MethodSource("notVolumes")
  void fromStringReturnsOffWhenMatchNotFound(String value) {
    var volume = Volume.fromString(value);
    assertEquals(Volume.OFF, volume);
  }

  private static Stream<Arguments> notVolumes() {
    return Stream.of(Arguments.of("not-a-volume"), Arguments.of((String) null));
  }
}
