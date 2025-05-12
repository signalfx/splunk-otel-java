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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * The intent of the pipeline is to select roughly 5% of traces for snapshotting, and this 5% should
 * be relatively stable regardless of how long we observe the snapshotting selection process. This
 * is a naturally non-deterministic behavior, however, we are not able to test it through
 * traditional deterministic unit tests.
 *
 * <p>Rather we need to test the behaviour by gathering a series of results and examining their
 * variance from the intent, and there are two data points in particular we are interested in 1.
 * Total percentage of traces selected for snapshotting 2. Stableness overtime of the snapshot
 * selection algorithm
 *
 * <p>We cannot expect exactly 5% of traces selected, nor can we expect a perfectly stable system.
 * We must instead accept some level of deviation from the stated intent of 5%.
 *
 * <p>Since the selection process is non-deterministic the idea behind this "test" is to confirm the
 * results are all within some acceptable range and to do this we need execute the program a lot of
 * times to obtain a distribution of outcomes. Here we have arbitrarily defined "a lot of times" to
 * mean 100.
 *
 * <p>Each run of the test will process 1000 traces and our expectation is for roughly 5%, or 100,
 * of those traces to be selected for snapshotting. We have arbitrarily defined our acceptable range
 * as 10% of our expectation, or 90 <= x <= 110.
 *
 * <p>However outcomes beyond the acceptable range are still expected, but we shouldn't expect too
 * many of those so our test assertions state that after the 100 executions are complete less than
 * 5% of outcomes are beyond the acceptable range. Or, put another way, at least 95% of outcomes are
 * within the acceptable range.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProbabilisticSnapshotSelectorTest {
  private static final int ITERATIONS = 100;
  private static final int TRACES_PER_ITERATIONS = 1000;
  private static final double SELECTION_RATE = 0.05;
  private static final List<Integer> OUTCOMES = new ArrayList<>();

  private final ProbabilisticSnapshotSelector selector =
      new ProbabilisticSnapshotSelector(SELECTION_RATE);

  @RepeatedTest(ITERATIONS)
  @Order(1)
  void processTraces() {
    int selected = 0;
    for (int i = 0; i < TRACES_PER_ITERATIONS; i++) {
      if (selector.select(Context.root())) {
        selected++;
      }
    }
    OUTCOMES.add(selected);
  }

  @Test
  @Order(2)
  void evaluatePercentAllowed() {
    assertEquals(SELECTION_RATE * 100, percentAllowed(), 1.0);
  }

  private double percentAllowed() {
    int allowed = OUTCOMES.stream().mapToInt(Integer::intValue).sum();
    int considered = TRACES_PER_ITERATIONS * OUTCOMES.size();
    return ((double) allowed / (double) (considered)) * 100;
  }

  @Test
  @Order(3)
  @Disabled("flaky test")
  void evaluateOutliers() {
    assertThat(outliers()).isLessThan((int) (TRACES_PER_ITERATIONS * SELECTION_RATE));
  }

  private long outliers() {
    double expectedSelectionsPerIteration = TRACES_PER_ITERATIONS * SELECTION_RATE;
    int upper = (int) (expectedSelectionsPerIteration * 1.1);
    int lower = (int) (expectedSelectionsPerIteration * .9);
    return OUTCOMES.stream().filter(i -> i < lower || i > upper).count();
  }
}
