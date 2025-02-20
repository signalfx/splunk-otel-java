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

import static com.splunk.opentelemetry.appd.AppdBonusConstants.CONFIG_CISCO_CTX_ENABLED;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.incubating.DeploymentIncubatingAttributes.DEPLOYMENT_ENVIRONMENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.BiFunction;
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

    when(config.getBoolean(CONFIG_CISCO_CTX_ENABLED, false)).thenReturn(true);
    when(resource.getAttribute(SERVICE_NAME)).thenReturn("amazingService");
    when(resource.getAttribute(DEPLOYMENT_ENVIRONMENT_NAME)).thenReturn("amazingEnv");

    AppdBonusCustomizer testClass = new AppdBonusCustomizer();
    testClass.customize(customizer, propagator);

    ArgumentCaptor<BiFunction<SpanProcessor, ConfigProperties, SpanProcessor>> spcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addSpanProcessorCustomizer(spcCaptor.capture());

    ArgumentCaptor<BiFunction<Resource, ConfigProperties, Resource>> rcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addResourceCustomizer(rcCaptor.capture());

    ArgumentCaptor<BiFunction<TextMapPropagator, ConfigProperties, TextMapPropagator>> pcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addPropagatorCustomizer(pcCaptor.capture());

    SpanProcessor spanProcessor = spcCaptor.getValue().apply(defaultSpanProcessor, config);
    assertThat(spanProcessor).isNotSameAs(defaultSpanProcessor);
    defaultSpanProcessor.onStart(mock(), mock());
    verify(defaultSpanProcessor).onStart(any(), any());

    Resource res = rcCaptor.getValue().apply(resource, config);
    assertThat(res).isSameAs(resource);
    verify(propagator).setServiceName("amazingService");
    verify(propagator).setEnvironmentName("amazingEnv");

    TextMapPropagator defaultPropagator = mock();
    TextMapPropagator prop = pcCaptor.getValue().apply(defaultPropagator, config);
    assertThat(prop).isNotSameAs(defaultPropagator);
    prop.extract(context, mock(), mock());
    verify(defaultPropagator).extract(any(), any(), any());
    verify(propagator).extract(any(), any(), any());
  }

  @Test
  void customizeNotEnabled() {
    when(config.getBoolean(CONFIG_CISCO_CTX_ENABLED, false)).thenReturn(false);

    AppdBonusCustomizer testClass = new AppdBonusCustomizer();
    testClass.customize(customizer, propagator);

    ArgumentCaptor<BiFunction<SpanProcessor, ConfigProperties, SpanProcessor>> spcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addSpanProcessorCustomizer(spcCaptor.capture());

    ArgumentCaptor<BiFunction<Resource, ConfigProperties, Resource>> rcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addResourceCustomizer(rcCaptor.capture());

    ArgumentCaptor<BiFunction<TextMapPropagator, ConfigProperties, TextMapPropagator>> pcCaptor =
        ArgumentCaptor.forClass(BiFunction.class);
    verify(customizer).addPropagatorCustomizer(pcCaptor.capture());

    SpanProcessor spanProcessor = spcCaptor.getValue().apply(defaultSpanProcessor, config);
    assertThat(spanProcessor).isSameAs(defaultSpanProcessor);

    Resource res = rcCaptor.getValue().apply(resource, config);
    assertThat(res).isSameAs(resource);
    verify(propagator, never()).setServiceName(anyString());
    verify(propagator, never()).setEnvironmentName(anyString());

    TextMapPropagator defaultPropagator = mock();
    TextMapPropagator prop = pcCaptor.getValue().apply(defaultPropagator, config);
    assertThat(prop).isSameAs(defaultPropagator);
    prop.extract(context, mock(), mock());
    verify(defaultPropagator).extract(any(), any(), any());
    verify(propagator, never()).extract(any(), any(), any());
  }
}
