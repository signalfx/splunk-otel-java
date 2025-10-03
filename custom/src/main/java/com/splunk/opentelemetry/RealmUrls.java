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

package com.splunk.opentelemetry;

public final class RealmUrls {

  private final MODE mode;

  public static final RealmUrls INSTANCE = legacy();

  private static RealmUrls legacy() {
    return new RealmUrls(MODE.SFX_LEGACY);
  }

  private static RealmUrls current() {
    throw new UnsupportedOperationException("New realms not supported yet.");
    // TODO: Uncomment when ready
    // return new RealmUrls(MODE.SFX_LEGACY);
  }

  private RealmUrls(MODE mode) {
    this.mode = mode;
  }

  public String otlpEndpoint(String realm) {
    if (mode == MODE.SFX_LEGACY) {
      return "https://ingest." + realm + ".signalfx.com";
    }
    return "https://ingest." + realm + ".observability.splunkcloud.com";
  }

  public String otlpMetrics(String realm) {
    if (mode == MODE.SFX_LEGACY) {
      return "https://ingest." + realm + ".signalfx.com/v2/datapoint/otlp";
    }
    return "https://ingest." + realm + ".observability.splunkcloud.com/v2/datapoint/otlp";
  }

  public String otlpLogs(String realm) {
    if (mode == MODE.SFX_LEGACY) {
      return "https://ingest." + realm + ".signalfx.com"; // ???
    }
    // return "https://ingest." + realm + ".observability.splunkcloud.com/v1/log";
    // Unclear why we didn't include a path before
    return "https://ingest." + realm + ".observability.splunkcloud.com";
  }

  public boolean isIngestUrl(String url) {
    return url.startsWith("https://ingest.")
        && (url.endsWith(".signalfx.com") || url.endsWith(".observability.splunkcloud.com"));
  }

  private enum MODE {
    SFX_LEGACY,
    CURRENT
  }
}
