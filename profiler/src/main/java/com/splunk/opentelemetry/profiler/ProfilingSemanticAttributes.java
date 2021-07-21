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

package com.splunk.opentelemetry.profiler;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

public class ProfilingSemanticAttributes {

  // NOTE: When we update to upstream 1.4.0+ this can be found on ResourceAttributes and this can be
  // removed
  public static final String SCHEMA_URL = "https://opentelemetry.io/schemas/1.4.0";

  public static final AttributeKey<String> LINKED_SPAN_ID = stringKey("span_id");
  public static final AttributeKey<String> LINKED_TRACE_ID = stringKey("trace_id");

  /** This is a HEC field that shows up in the Logging UI. */
  public static final AttributeKey<String> SOURCE_TYPE = stringKey("com.splunk.sourcetype");

  /** The name of the originating event that generated this profiling event */
  public static final AttributeKey<String> SOURCE_EVENT_NAME = stringKey("source.event.name");

  /** The period of the source event, in ms. */
  public static final AttributeKey<Long> SOURCE_EVENT_PERIOD = longKey("source.event.period");
}
