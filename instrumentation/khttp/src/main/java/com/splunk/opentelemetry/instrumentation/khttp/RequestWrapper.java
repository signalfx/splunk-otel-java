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

package com.splunk.opentelemetry.instrumentation.khttp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class RequestWrapper {
  final String method;
  final String uri;
  final Map<String, String> headers;
  final URI parsedUri;

  public RequestWrapper(String method, String uri, Map<String, String> headers) {
    this.method = method;
    this.uri = uri;
    this.headers = headers;
    this.parsedUri = parseUri(uri);
  }

  private static URI parseUri(String uri) {
    if (uri == null) {
      return null;
    }
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      return null;
    }
  }
}
