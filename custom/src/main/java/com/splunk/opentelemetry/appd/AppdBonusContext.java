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

import javax.annotation.Nullable;

public class AppdBonusContext {
  @Nullable private final String appId;
  @Nullable private final String accountId;
  @Nullable private final String businessTransactionId;
  @Nullable private final String tierId;

  public AppdBonusContext(
      @Nullable String accountId,
      @Nullable String appId,
      @Nullable String businessTransactionId,
      @Nullable String tierId) {
    this.accountId = accountId;
    this.appId = appId;
    this.businessTransactionId = businessTransactionId;
    this.tierId = tierId;
  }

  @Nullable
  public String getAccountId() {
    return accountId;
  }

  @Nullable
  public String getAppId() {
    return appId;
  }

  @Nullable
  public String getBusinessTransactionId() {
    return businessTransactionId;
  }

  @Nullable
  public String getTierId() {
    return tierId;
  }
}
