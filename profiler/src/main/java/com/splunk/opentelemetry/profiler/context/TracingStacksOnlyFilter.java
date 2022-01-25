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

package com.splunk.opentelemetry.profiler.context;

import com.splunk.opentelemetry.profiler.StackTracePredicate;

public class TracingStacksOnlyFilter implements StackTracePredicate {
  private final StackTracePredicate delegate;
  private final SpanContextualizer contextualizer;

  public TracingStacksOnlyFilter(StackTracePredicate delegate, SpanContextualizer contextualizer) {
    this.delegate = delegate;
    this.contextualizer = contextualizer;
  }

  @Override
  public boolean test(String wallOfStacks, int startIndex, int lastIndex) {
    if (!delegate.test(wallOfStacks, startIndex, lastIndex)) {
      return false;
    }

    // Switch from "last index" (inclusive) to "end index" (exclusive), thus the +1
    return contextualizer.link(wallOfStacks, startIndex, lastIndex + 1).getSpanContext().isValid();
  }
}
