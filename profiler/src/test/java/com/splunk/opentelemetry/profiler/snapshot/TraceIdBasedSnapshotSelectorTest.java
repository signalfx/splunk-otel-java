package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TraceIdBasedSnapshotSelectorTest {
  @Test
  void doNotSelectTraceWhenRoot() {
    var context = Context.root();
    var selector = new TraceIdBasedSnapshotSelector(0.05);
    assertThat(selector.select(context)).isFalse();
  }

  @ParameterizedTest
  @MethodSource("traceIdsToSelect")
  void selectTraceWhenTraceIdIsComputedToBeLessThanOrEqualToSelectionRate(String traceId) {
    var spanContext = Snapshotting.spanContext().withTraceId(traceId).build();
    var span = Span.wrap(spanContext);
    var context = Context.root().with(span);

    var selector = new TraceIdBasedSnapshotSelector(0.05);
    assertThat(selector.select(context)).isTrue();
  }

  /**
   * Trace IDs with a note about what the expected computed value is
   */
  private static Stream<String> traceIdsToSelect() {
    return IntStream.range(-5, 6).mapToObj(SpecialTraceIds::forPercentile);
  }

  @ParameterizedTest
  @MethodSource("traceIdsToNotSelect")
  void doNotSelectTraceWhenTraceIdIsComputedToBeMoreThanSelectionRate(String traceId) {
    var spanContext = Snapshotting.spanContext().withTraceId(traceId).build();
    var span = Span.wrap(spanContext);
    var context = Context.root().with(span);

    var selector = new TraceIdBasedSnapshotSelector(0.05);
    assertThat(selector.select(context)).isFalse();
  }

  /**
   * Trace IDs with a note about what the expected computed value is
   */
  private static Stream<String> traceIdsToNotSelect() {
    return Stream.concat(
        IntStream.range(-99, -5).mapToObj(SpecialTraceIds::forPercentile),
        IntStream.range(6, 100).mapToObj(SpecialTraceIds::forPercentile)
    );
  }
}
