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

package com.splunk.opentelemetry.jaeger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Interceptor.Chain;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthTokenInterceptorTest {
  @Mock Chain chain;
  @Captor ArgumentCaptor<Request> requestCaptor;

  Interceptor interceptor = new AuthTokenInterceptor("token");

  @Test
  void shouldAddSignalFxToken() throws IOException {
    // given
    var request = new Request.Builder().url("http://test").build();
    given(chain.request()).willReturn(request);

    // when
    interceptor.intercept(chain);

    // then
    then(chain).should().proceed(requestCaptor.capture());
    assertEquals("token", requestCaptor.getValue().header(AuthTokenInterceptor.TOKEN_HEADER));
  }
}
