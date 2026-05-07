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

package com.splunk.opentelemetry.opamp;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.valueKey;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAMESPACE;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class OpampAgentAttributesTest {

  private static final Attributes resourceAttributes =
      Attributes.of(
              SERVICE_NAME,
              "test-service",
              SERVICE_NAMESPACE,
              "test-namespace",
              SERVICE_INSTANCE_ID,
              "test-instance",
              SERVICE_VERSION,
              "test-version")
          .toBuilder()
          .put(longKey("long"), 12L)
          .put(doubleKey("double"), 99.0)
          .put(booleanKey("bool"), true)
          .put(valueKey("val"), Value.of("vvv"))
          .put("longarr", new long[] {2L, 3L, 5L})
          .put(AttributeKey.longArrayKey("longobjarr"), Arrays.asList(2L, 3L, 5L))
          .put("doublearr", new double[] {2.0, 3.0})
          .put(AttributeKey.doubleArrayKey("doubleobjarr"), Arrays.asList(5.0, 6.0))
          .put("stringarr", new String[] {"foo", "flimflam"})
          .put(AttributeKey.stringArrayKey("stringobjarr"), Arrays.asList("flim", "jibberjo"))
          .put("boolarr", new boolean[] {true, false})
          .put(AttributeKey.booleanArrayKey("boolobjarr"), Arrays.asList(true, true, false, true))
          .build();
  private static final Resource resource = Resource.create(resourceAttributes);

  @Test
  void addsIdentifyingAttributes() {
    OpampClientBuilder builder = mock(OpampClientBuilder.class);

    OpampAgentAttributes testClass = new OpampAgentAttributes(resource);
    testClass.addIdentifyingAttributes(builder);

    verify(builder).putIdentifyingAttribute(SERVICE_NAME.getKey(), "test-service");
    verify(builder).putIdentifyingAttribute(SERVICE_NAMESPACE.getKey(), "test-namespace");
    verify(builder).putIdentifyingAttribute(SERVICE_INSTANCE_ID.getKey(), "test-instance");
    verifyNoMoreInteractions(builder);
  }

  @Test
  void addsNonIdentifyingAttributes() {
    OpampClientBuilder builder = mock();

    OpampAgentAttributes testClass = new OpampAgentAttributes(resource);

    testClass.addNonIdentifyingAttributes(builder);

    verify(builder).putNonIdentifyingAttribute(SERVICE_VERSION.getKey(), "test-version");
    verify(builder).putNonIdentifyingAttribute("long", 12L);
    verify(builder).putNonIdentifyingAttribute("double", 99.0);
    verify(builder).putNonIdentifyingAttribute("bool", true);
    verify(builder).putNonIdentifyingAttribute("val", "vvv");

    verify(builder).putNonIdentifyingAttribute("longarr", 2L, 3L, 5L);
    verify(builder).putNonIdentifyingAttribute("longobjarr", 2L, 3L, 5L);
    verify(builder).putNonIdentifyingAttribute("doublearr", 2.0, 3.0);
    verify(builder).putNonIdentifyingAttribute("doubleobjarr", 5.0, 6.0);
    verify(builder).putNonIdentifyingAttribute("stringarr", "foo", "flimflam");
    verify(builder).putNonIdentifyingAttribute("stringobjarr", "flim", "jibberjo");
    verify(builder).putNonIdentifyingAttribute("boolarr", true, false);
    verify(builder).putNonIdentifyingAttribute("boolobjarr", true, true, false, true);
    verifyNoMoreInteractions(builder);
  }
}
