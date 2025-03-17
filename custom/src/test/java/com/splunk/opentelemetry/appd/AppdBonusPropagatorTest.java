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

import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CONTEXT_KEY;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_ACCT;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_APP;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_BT;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_ENV;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_SERVICE;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CTX_HEADER_TIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AppdBonusPropagatorTest {

  @Test
  void injectBeforeSet() {
    Map<String, String> map = new HashMap<>();
    Context context = mock();

    AppdBonusPropagator testClass = AppdBonusPropagator.getInstance();
    testClass.setEnvironmentName(null);
    testClass.setServiceName(null);

    testClass.inject(context, map, Map::put);

    assertThat(map).isEmpty();
  }

  @Test
  void inject() {
    Context context = mock();
    Map<String, String> map = new HashMap<>();

    AppdBonusPropagator testClass = AppdBonusPropagator.getInstance();
    testClass.setServiceName("xxxservice");
    testClass.setEnvironmentName("xxxenv");
    testClass.inject(context, map, Map::put);

    assertThat(map.get(CTX_HEADER_ENV)).isEqualTo("xxxenv");
    assertThat(map.get(CTX_HEADER_SERVICE)).isEqualTo("xxxservice");
  }

  @Test
  void extract() {
    Context context = mock();
    Map<String, String> map = new HashMap<>();
    // insert fake headers
    map.put(CTX_HEADER_BT, "123bt");
    map.put(CTX_HEADER_APP, "123app");
    map.put(CTX_HEADER_TIER, "123tier");
    map.put(CTX_HEADER_ACCT, "123acct");

    AppdBonusPropagator testClass = AppdBonusPropagator.getInstance();
    testClass.extract(
        context,
        map,
        new TextMapGetter<>() {
          @Override
          public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
          }

          @Nullable
          @Override
          public String get(@Nullable Map<String, String> carrier, String key) {
            return carrier.get(key);
          }
        });
    ArgumentCaptor<AppdBonusContext> data = ArgumentCaptor.forClass(AppdBonusContext.class);
    verify(context).with(eq(CONTEXT_KEY), data.capture());
    AppdBonusContext result = data.getValue();
    assertThat(result.getAccountId()).isEqualTo("123acct");
    assertThat(result.getTierId()).isEqualTo("123tier");
    assertThat(result.getAppId()).isEqualTo("123app");
    assertThat(result.getBusinessTransactionId()).isEqualTo("123bt");
  }

  @Test
  void fields() {
    AppdBonusPropagator testClass = AppdBonusPropagator.getInstance();
    assertThat(testClass.fields())
        .containsExactly(
            CTX_HEADER_ACCT,
            CTX_HEADER_APP,
            CTX_HEADER_BT,
            CTX_HEADER_TIER,
            CTX_HEADER_ENV,
            CTX_HEADER_SERVICE);
  }
}
