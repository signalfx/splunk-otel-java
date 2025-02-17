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

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

public class ProfilingSemanticAttributes {

  /** This is a HEC field that shows up in the Logging UI. */
  public static final AttributeKey<String> SOURCE_TYPE = stringKey("com.splunk.sourcetype");

  /** The name of the originating event that generated this profiling event */
  public static final AttributeKey<String> SOURCE_EVENT_NAME = stringKey("source.event.name");

  /** The period of the source event, in ms. */
  public static final AttributeKey<Long> SOURCE_EVENT_PERIOD = longKey("source.event.period");

  public static final AttributeKey<Long> SOURCE_EVENT_TIME = longKey("source.event.time");

  public static final AttributeKey<String> DATA_TYPE = stringKey("profiling.data.type");
  public static final AttributeKey<String> DATA_FORMAT = stringKey("profiling.data.format");
  public static final String PPROF_GZIP_BASE64 = "pprof-gzip-base64";
  public static final AttributeKey<Long> FRAME_COUNT = longKey("profiling.data.total.frame.count");
  public static final AttributeKey<String> INSTRUMENTATION_SOURCE =
      stringKey("profiling.instrumentation.source");

  public static final AttributeKey<Long> THREAD_ID = longKey("thread.id");
  public static final AttributeKey<String> THREAD_NAME = stringKey("thread.name");
  public static final AttributeKey<String> THREAD_STATE = stringKey("thread.state");
  public static final AttributeKey<Boolean> THREAD_STACK_TRUNCATED =
      booleanKey("thread.stack.truncated");

  public static final AttributeKey<String> TRACE_ID = stringKey("trace_id");
  public static final AttributeKey<String> SPAN_ID = stringKey("span_id");

  public static final String PROFILING_SOURCE = "otel.profiling";

  public static final String OTEL_INSTRUMENTATION_NAME = "otel.profiling";
  public static final String OTEL_INSTRUMENTATION_VERSION = "0.1.0";
}
