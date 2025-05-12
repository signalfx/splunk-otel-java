package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.context.Context;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SnapshotSelectorTest {
  private static final SnapshotSelector TRUE = c -> true;
  private static final SnapshotSelector FALSE = c -> false;

  @ParameterizedTest
  @MethodSource("oneOrTheOther")
  void orEvaluatesCorrectly(SnapshotSelector selector, boolean expected) {
    var context = Context.root();
    assertThat(selector.select(context)).isEqualTo(expected);
  }

  private static Stream<Arguments> oneOrTheOther() {
    return Stream.of(
        Arguments.of(TRUE.or(TRUE), true),
        Arguments.of(TRUE.or(FALSE), true),
        Arguments.of(FALSE.or(TRUE), true),
        Arguments.of(FALSE.or(FALSE), false)
    );
  }
}
