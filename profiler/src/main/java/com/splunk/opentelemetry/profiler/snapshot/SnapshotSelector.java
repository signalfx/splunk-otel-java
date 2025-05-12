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

import io.opentelemetry.context.Context;
import java.util.function.Predicate;

interface SnapshotSelector extends Predicate<Context> {
  default SnapshotSelector or(SnapshotSelector other) {
    return context -> select(context) || other.select(context);
  }

  default boolean test(Context context) {
    return select(context);
  }

  boolean select(Context context);
}
