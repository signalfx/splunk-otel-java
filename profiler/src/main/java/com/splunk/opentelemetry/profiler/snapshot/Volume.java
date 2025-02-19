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

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ImplicitContextKeyed;
import java.util.Locale;

public enum Volume implements ImplicitContextKeyed {
  OFF,
  HIGHEST;

  private static final String SPLUNK_TRACE_SNAPSHOT_VOLUME = "splunk.trace.snapshot.volume";

  static Volume from(Context context) {
    Baggage baggage = Baggage.fromContext(context);
    return fromString(baggage.getEntryValue(SPLUNK_TRACE_SNAPSHOT_VOLUME));
  }

  private static Volume fromString(String value) {
    if (value == null) {
      return OFF;
    }

    try {
      return Volume.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return OFF;
    }
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }

  @Override
  public Context storeInContext(Context context) {
    Baggage baggage = Baggage.builder().put(SPLUNK_TRACE_SNAPSHOT_VOLUME, toString()).build();
    return context.with(baggage);
  }
}
