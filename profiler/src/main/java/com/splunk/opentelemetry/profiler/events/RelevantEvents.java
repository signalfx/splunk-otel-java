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

package com.splunk.opentelemetry.profiler.events;

import com.splunk.opentelemetry.profiler.TLABProcessor;
import com.splunk.opentelemetry.profiler.ThreadDumpProcessor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RelevantEvents {
  public static Set<String> EVENT_NAMES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  ThreadDumpProcessor.EVENT_NAME,
                  ContextAttached.EVENT_NAME,
                  TLABProcessor.NEW_TLAB_EVENT_NAME,
                  TLABProcessor.OUTSIDE_TLAB_EVENT_NAME)));
}
