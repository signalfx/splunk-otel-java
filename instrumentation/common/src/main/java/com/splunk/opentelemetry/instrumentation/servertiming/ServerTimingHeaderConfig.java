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

package com.splunk.opentelemetry.instrumentation.servertiming;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

public final class ServerTimingHeaderConfig {

  private static final String EMIT_RESPONSE_HEADERS = "splunk.trace-response-header.enabled";

  private ServerTimingHeaderConfig() {}

  // needs to be in a separate class to appease muzzle
  public static boolean shouldEmitServerTimingHeader(ConfigProperties config) {
    return config.getBoolean(EMIT_RESPONSE_HEADERS, true);
  }
}
