package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class VolumeTest {
  @ParameterizedTest
  @MethodSource("volumesAsStrings")
  void toStringRepresentation(Volume volume, String asString) {
    assertEquals(asString, volume.toString());
  }

  @ParameterizedTest
  @MethodSource("volumesAsStrings")
  void fromStringRepresentation(Volume volume, String asString) {
    assertEquals(volume, Volume.fromString(asString));
  }

  private static Stream<Arguments> volumesAsStrings() {
    return Stream.of(
        Arguments.of(Volume.OFF, "off"),
        Arguments.of(Volume.HIGHEST, "highest")
    );
  }

  @ParameterizedTest
  @MethodSource("notVolumes")
  void fromStringReturnsOffWhenMatchNotFound(String value) {
    var volume = Volume.fromString(value);
    assertEquals(Volume.OFF, volume);
  }

  private static Stream<Arguments> notVolumes() {
    return Stream.of(
        Arguments.of("not-a-volume"),
        Arguments.of((String)null)
    );
  }
}
