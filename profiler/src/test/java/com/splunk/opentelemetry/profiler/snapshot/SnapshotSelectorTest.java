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

import io.opentelemetry.context.Context;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
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
        Arguments.of(FALSE.or(FALSE), false));
  }

  @Test
  void orReturnsImmediatelyUponPositiveSelection() {
    var context = Context.root();

    var first = new ObservableSelector(true);
    var second = new ObservableSelector(false);
    var selector = first.or(second);

    assertThat(selector.select(context)).isTrue();
    assertThat(first.evaluated).isTrue();
    assertThat(second.evaluated).isFalse();
  }

  static class ObservableSelector implements SnapshotSelector {
    private final boolean selection;
    private boolean evaluated = false;

    ObservableSelector(boolean selection) {
      this.selection = selection;
    }

    @Override
    public boolean select(Context context) {
      evaluated = true;
      return selection;
    }
  }
}
