package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
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
    return Stream.of(
        "a9be2b13c837f54c76ce24cd934d6935", // -5
        "b857e01b37ef63ed8d53727bf964dbd0", // -4
        "0a9e78eb7cb2aa49058015da8dca716a", // -3
        "00e52141255016019c9103c436625f48", // -2
        "62ee39067afd2312ffe7d3e29d7627ee", // -1
        "f1a06d523139f261dbbcab5446338de8", //  0
        "a8f977212c67f101f0cfd1adafaa405a", //  1
        "245a990fb1d65fcbb030bbc64cde8ce5", //  2
        "9e43f9879b3565e358c11a61bf4f2011", //  3
        "fb5fda53097dc4bc86a0766932d8ec51", //  4
        "17dd4685ab52acabfdc3fa383df3fe88"  //  5
    );
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
    return Stream.of(
        "cf6f99dc7782965d19468011a13aac1a", // -10
        "9e4cb36fd315c2ac241d32bd2dbd915f", //  -9
        "05d340218beffc21dbeef4cb15dceabb", //  -8
        "54423fbcac5b2e60913ef857c346229a", //  -7
        "1d119d4d4b4836965957ab42796a6a78", //  -6
        "0881ffca9ae845bb7de6bd88d026bfe4", //   6
        "eaf5aa91823f23e2ff208d15869e2593", //   7
        "901f7780b5fbbfe518f5d2bac4599222", //   8
        "1d8d85992b12e546713fb02994f91234", //   9
        "9b79391544970bf7d31dd0254b4449a9"  //  10
    );
  }
}
