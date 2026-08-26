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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TraceIdBasedSnapshotSelectorTest {
  @ParameterizedTest
  @ValueSource(doubles = {-0.01, 1.01})
  void requireSelectionRateBetween0_0And1_0(double selectionRate) {
    assertThrows(
        IllegalArgumentException.class, () -> new TraceIdBasedSnapshotSelector(selectionRate));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.01, 1.01})
  void requireUpdatedSelectionRateBetween0_0And1_0(double selectionRate) {
    var selector = new TraceIdBasedSnapshotSelector(0.5);

    assertThrows(
        IllegalArgumentException.class,
        () -> selector.setSnapshotSelectionProbability(selectionRate));
  }

  @Test
  void doSelectTraceWhenRoot() {
    var spanContext =
        Snapshotting.spanContext().withTraceId("00000000000000004adb3965a6cbec2d").build();
    var selector = new TraceIdBasedSnapshotSelector(1.0);
    assertThat(selector.select(spanContext)).isTrue();
  }

  @Test
  void useUpdatedSelectionProbability() {
    var spanContext =
        Snapshotting.spanContext()
            .withTraceId(SnapshotSelectorTestTraceIds.forPercentile(6).get(0))
            .build();
    var selector = new TraceIdBasedSnapshotSelector(0.05);

    assertThat(selector.select(spanContext)).isFalse();

    selector.setSnapshotSelectionProbability(0.06);

    assertThat(selector.select(spanContext)).isTrue();
  }

  @Test
  void doNotSelectTraceAfterProbabilityIsUpdatedToZero() {
    var spanContext =
        Snapshotting.spanContext().withTraceId("00000000000000004adb3965a6cbec2d").build();
    var selector = new TraceIdBasedSnapshotSelector(1.0);

    selector.setSnapshotSelectionProbability(0.0);

    assertThat(selector.select(spanContext)).isFalse();
  }

  @ParameterizedTest
  @MethodSource("traceIdsToSelect")
  void selectTraceWhenTraceIdIsComputedToBeLessThanOrEqualToSelectionRate(String traceId) {
    var spanContext = Snapshotting.spanContext().withTraceId(traceId).build();

    var selector = new TraceIdBasedSnapshotSelector(0.05);
    assertThat(selector.select(spanContext)).isTrue();
  }

  private static Stream<String> traceIdsToSelect() {
    return IntStream.range(1, 6)
        .mapToObj(SnapshotSelectorTestTraceIds::forPercentile)
        .flatMap(List::stream);
  }

  @ParameterizedTest
  @MethodSource("traceIdsToNotSelect")
  void doNotSelectTraceWhenTraceIdIsComputedToBeMoreThanSelectionRate(String traceId) {
    var spanContext = Snapshotting.spanContext().withTraceId(traceId).build();

    var selector = new TraceIdBasedSnapshotSelector(0.05);
    assertThat(selector.select(spanContext)).isFalse();
  }

  private static Stream<String> traceIdsToNotSelect() {
    return IntStream.range(6, 100)
        .mapToObj(SnapshotSelectorTestTraceIds::forPercentile)
        .flatMap(List::stream);
  }
}
