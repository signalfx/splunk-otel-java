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

package com.splunk.opentelemetry.appd;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

public final class AppdBonusConstants {
  public static final String CONFIG_CISCO_CTX_ENABLED = "cisco.ctx.enabled";
  public static final String PROPAGATOR_NAME = "appd-bonus";
  public static final String CTX_KEY = "cisco-bonus-ctx";
  public static final String CTX_HEADER_ACCT = "cisco-ctx-acct-id";
  public static final String CTX_HEADER_APP = "cisco-ctx-app-id";
  public static final String CTX_HEADER_BT = "cisco-ctx-bt-id";
  public static final String CTX_HEADER_TIER = "cisco-ctx-tier-id";
  public static final String CTX_HEADER_ENV = "cisco-ctx-env";
  public static final String CTX_HEADER_SERVICE = "cisco-ctx-service";
  public static final AttributeKey<String> APPD_ATTR_ACCT = stringKey("appd.upstream.account.id");
  public static final AttributeKey<String> APPD_ATTR_APP = stringKey("appd.upstream.app.id");
  public static final AttributeKey<String> APPD_ATTR_BT = stringKey("appd.upstream.bt.id");
  public static final AttributeKey<String> APPD_ATTR_TIER = stringKey("appd.upstream.tier.id");

  private AppdBonusConstants() {}
}
