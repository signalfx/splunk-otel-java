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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.BeforeAgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.function.Consumer;
import java.util.logging.Logger;

@AutoService(BeforeAgentListener.class)
public class RealmAccessTokenChecker implements BeforeAgentListener {
  private static final Logger logger = Logger.getLogger(RealmAccessTokenChecker.class.getName());

  private final Consumer<String> logWarn;

  @SuppressWarnings("unused")
  public RealmAccessTokenChecker() {
    this(logger::warning);
  }

  // visible for tests
  RealmAccessTokenChecker(Consumer<String> logWarn) {
    this.logWarn = logWarn;
  }

  @Override
  public void beforeAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = getConfig(autoConfiguredOpenTelemetrySdk);

    if (isRealmConfigured(config) && !isAccessTokenConfigured(config)) {
      // TODO: For declarative configuration message wording should be different
      logWarn.accept(
          "Splunk realm is defined, which sets the default endpoint URLs to Splunk ingest URLs. However, access token is not defined, which is required for those endpoints. Please set the access token using the 'SPLUNK_ACCESS_TOKEN' environment variable or the 'splunk.access.token' system property.");
    }
  }

  // make sure this listener is one of the first things run by the agent
  @Override
  public int order() {
    return -100;
  }

  private static boolean isRealmConfigured(ConfigProperties config) {
    String realm =
        config.getString(
            SplunkConfiguration.SPLUNK_REALM_PROPERTY, SplunkConfiguration.SPLUNK_REALM_NONE);
    return !realm.equals(SplunkConfiguration.SPLUNK_REALM_NONE);
  }

  private static boolean isAccessTokenConfigured(ConfigProperties config) {
    return config.getString(SplunkConfiguration.SPLUNK_ACCESS_TOKEN) != null;
  }
}
