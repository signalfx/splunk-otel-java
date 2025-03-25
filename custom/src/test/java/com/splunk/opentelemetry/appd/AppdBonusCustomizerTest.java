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

import static com.splunk.opentelemetry.appd.AppdBonusCustomizer.DEFAULT_PROPAGATORS;
import static com.splunk.opentelemetry.appd.AppdBonusPropagator.CONTEXT_KEY;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_ACCT;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_APP;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_BT;
import static com.splunk.opentelemetry.appd.AppdBonusSpanProcessor.APPD_ATTR_TIER;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppdBonusCustomizerTest {

  @Mock AutoConfigurationCustomizer customizer;
  @Mock ConfigProperties config;
  @Mock Context context;
  @Mock AppdBonusPropagator propagator;
  @Mock SpanProcessor defaultSpanProcessor;
  @Mock Resource resource;

  @Test
  void customizeEnabled() {
    AppdBonusContext appdContext = new AppdBonusContext("acct123", "app456", "bt123", "tier456");

    when(context.get(CONTEXT_KEY)).thenReturn(appdContext);
    when(config.getBoolean("cisco.ctx.enabled", false)).thenReturn(true);
    when(config.getList("otel.propagators", DEFAULT_PROPAGATORS)).thenReturn(DEFAULT_PROPAGATORS);
    when(resource.getAttribute(SERVICE_NAME)).thenReturn("amazingService");
    when(resource.getAttribute(DEPLOYMENT_ENVIRONMENT_NAME)).thenReturn("amazingEnv");

    AppdBonusCustomizer testClass = new AppdBonusCustomizer();
    testClass.customize(customizer, propagator);

    ArgumentCaptor<Function<ConfigProperties, Map<String, String>>> pcCaptor =
        ArgumentCaptor.forClass(Function.class);
    verify(customizer).addPropertiesCustomizer(pcCaptor.capture());

    ArgumentCaptor<BiFunction<Resource, ConfigProperties, Resource>> rcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addResourceCustomizer(rcCaptor.capture());

    ArgumentCaptor<BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>>
        tpcCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addTracerProviderCustomizer(tpcCaptor.capture());

    Map<String, String> updatedConfig = pcCaptor.getValue().apply(config);
    assertThat(updatedConfig).containsEntry("otel.propagators", "tracecontext,baggage,appd-bonus");

    defaultSpanProcessor.onStart(mock(), mock());
    verify(defaultSpanProcessor).onStart(any(), any());

    Resource res = rcCaptor.getValue().apply(resource, config);
    assertThat(res).isSameAs(resource);
    verify(propagator).setServiceName("amazingService");
    verify(propagator).setEnvironmentName("amazingEnv");

    SdkTracerProviderBuilder builder = mock();
    tpcCaptor.getValue().apply(builder, config);
    ArgumentCaptor<SpanProcessor> spCapture = ArgumentCaptor.forClass(SpanProcessor.class);
    verify(builder).addSpanProcessor(spCapture.capture());

    ReadWriteSpan span = mock();
    SpanContext parentSpanContext = mock(SpanContext.class);
    when(parentSpanContext.isValid()).thenReturn(false);
    when(span.getParentSpanContext()).thenReturn(parentSpanContext);

    spCapture.getValue().onStart(context, span);

    verify(span).setAttribute(APPD_ATTR_APP, appdContext.getAppId());
    verify(span).setAttribute(APPD_ATTR_ACCT, appdContext.getAccountId());
    verify(span).setAttribute(APPD_ATTR_TIER, appdContext.getTierId());
    verify(span).setAttribute(APPD_ATTR_BT, appdContext.getBusinessTransactionId());
  }

  @Test
  void customizeNotEnabled() {
    when(config.getBoolean("cisco.ctx.enabled", false)).thenReturn(false);
    when(resource.getAttribute(SERVICE_NAME)).thenReturn("amazingService");
    when(resource.getAttribute(DEPLOYMENT_ENVIRONMENT_NAME)).thenReturn("amazingEnv");

    AppdBonusCustomizer testClass = new AppdBonusCustomizer();
    testClass.customize(customizer, propagator);

    ArgumentCaptor<Function<ConfigProperties, Map<String, String>>> pcCaptor =
        ArgumentCaptor.forClass(Function.class);
    verify(customizer).addPropertiesCustomizer(pcCaptor.capture());

    ArgumentCaptor<BiFunction<Resource, ConfigProperties, Resource>> rcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addResourceCustomizer(rcCaptor.capture());

    ArgumentCaptor<BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>>
        tpcCaptor = ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addTracerProviderCustomizer(tpcCaptor.capture());

    Map<String, String> updatedConfig = pcCaptor.getValue().apply(config);
    assertThat(updatedConfig).isEmpty();

    defaultSpanProcessor.onStart(mock(), mock());
    verify(defaultSpanProcessor).onStart(any(), any());

    Resource res = rcCaptor.getValue().apply(resource, config);
    assertThat(res).isSameAs(resource);
    verify(propagator).setServiceName("amazingService");
    verify(propagator).setEnvironmentName("amazingEnv");

    SdkTracerProviderBuilder builder = mock();
    tpcCaptor.getValue().apply(builder, config);
    verify(builder, never()).addSpanProcessor(any());
  }

  @Test
  void nonePreventsAddingPropagator() {
    when(config.getBoolean("cisco.ctx.enabled", false)).thenReturn(true);
    when(config.getList("otel.propagators", DEFAULT_PROPAGATORS)).thenReturn(List.of("none"));

    AppdBonusCustomizer testClass = new AppdBonusCustomizer();
    testClass.customize(customizer, propagator);

    ArgumentCaptor<Function<ConfigProperties, Map<String, String>>> pcCaptor =
        ArgumentCaptor.forClass(Function.class);
    verify(customizer).addPropertiesCustomizer(pcCaptor.capture());

    Map<String, String> updatedConfig = pcCaptor.getValue().apply(config);
    assertThat(updatedConfig).isEmpty();
  }
}
