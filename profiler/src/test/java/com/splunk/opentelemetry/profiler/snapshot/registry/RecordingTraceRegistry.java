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

package com.splunk.opentelemetry.profiler.snapshot.registry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test only version of {@link TraceRegistry} that keeps a record of every trace ID registered over
 * the lifetime of the instance.
 */
public class RecordingTraceRegistry extends TraceRegistry {
  private final Set<String> registeredTraceIds =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  @Override
  public void register(String traceId) {
    registeredTraceIds.add(traceId);
    super.register(traceId);
  }

  public Set<String> registeredTraceIds() {
    return Collections.unmodifiableSet(registeredTraceIds);
  }
}
