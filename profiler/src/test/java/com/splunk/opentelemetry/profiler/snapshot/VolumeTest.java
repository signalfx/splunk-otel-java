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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import java.util.Locale;
import java.util.stream.Stream;
import net.bytebuddy.utility.RandomString;
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
  void extractVolumeFromOpenTelemetryContext(Volume volume, String asString) {
    var baggage = Baggage.builder().put("splunk.trace.snapshot.volume", asString).build();
    var context = Context.current().with(baggage);
    assertEquals(volume, Volume.from(context));
  }

  @ParameterizedTest
  @MethodSource("volumesAsStrings")
  void fromContextIsNotSensitiveToLocale(Volume volume, String asString) {
    var baggage = Baggage.builder().put("splunk.trace.snapshot.volume", asString).build();
    var context = Context.current().with(baggage);

    var defaultLocale = Locale.getDefault();
    try {
      for (var locale : Locale.getAvailableLocales()) {
        Locale.setDefault(locale);
        assertEquals(volume, Volume.from(context));
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
  void fromContextReturnsOffWhenMatchNotFound(String value) {
    var baggage = Baggage.builder().put("splunk.trace.snapshot.volume", value).build();
    var context = Context.current().with(baggage);
    assertEquals(Volume.OFF, Volume.from(context));
  }

  private static Stream<Arguments> notVolumes() {
    return Stream.of(Arguments.of("not-a-volume"), Arguments.of((String) null));
  }

  @ParameterizedTest
  @EnumSource(Volume.class)
  void storeBaggageRepresentationInOpenTelemetryContext(Volume volume) {
    var context = Context.current().with(volume);
    var baggage = Baggage.fromContext(context);
    var entry = baggage.getEntry("splunk.trace.snapshot.volume");

    assertNotNull(entry);
    assertEquals(volume.toString(), entry.getValue());
  }

  @ParameterizedTest
  @EnumSource(Volume.class)
  void respectPreviousBaggageEntriesOpenTelemetryContext(Volume volume) {
    var baggageKey = "existing-baggage-entry";
    var baggageValue = RandomString.make();
    var context = Context.current().with(Baggage.builder().put(baggageKey, baggageValue).build());

    try (var ignored = context.makeCurrent()) {
      var contextWithVolume = context.with(volume);
      var baggage = Baggage.fromContext(contextWithVolume);
      var entry = baggage.getEntry(baggageKey);

      assertNotNull(entry);
      assertEquals(baggageValue, entry.getValue());
    }
  }
}
