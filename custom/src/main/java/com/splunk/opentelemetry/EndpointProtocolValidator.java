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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.function.Consumer;

class EndpointProtocolValidator {

  private final Map<String, String> customized;
  private final ConfigProperties config;
  private final Consumer<String> warn;

  private EndpointProtocolValidator(
      Map<String, String> customized, ConfigProperties config, Consumer<String> warn) {
    this.customized = customized;
    this.config = config;
    this.warn = warn;
  }

  /** Warn when the port number in OTLP endpoint doesn't agree with configured protocol. */
  static void validate(
      Map<String, String> customized, ConfigProperties config, Consumer<String> warn) {
    EndpointProtocolValidator validator = new EndpointProtocolValidator(customized, config, warn);
    validator.verifyEndpointProtocol();
  }

  private void verifyEndpointProtocol() {
    String protocol = getString("otel.exporter.otlp.protocol", "http/protobuf");
    String endpoint =
        getString(
            "otel.exporter.otlp.endpoint",
            "http://localhost:" + ("http/protobuf".equals(protocol) ? "4318" : "4317"));
    if (!verifyEndpointProtocol(
        protocol, endpoint, "otel.exporter.otlp.protocol", "otel.exporter.otlp.endpoint")) {
      // there already was a mismatch between endpoint port and protocol,
      // skip looking at signal specific endpoints
      return;
    }

    for (String signal : new String[] {"traces", "metrics", "logs"}) {
      verifySignalEndpointProtocol(signal, protocol, endpoint);
    }
  }

  private boolean verifyEndpointProtocol(
      String protocol, String endpoint, String protocolKey, String endpointKey) {
    int port = getEndpointPort(endpoint);
    if ("http/protobuf".equals(protocol)) {
      if (port == 4317) {
        warn.accept(protocolWarning("grpc", "http/protobuf", endpoint, protocolKey, endpointKey));
        return false;
      }
    } else if ("grpc".equals(protocol)) {
      if (port == 4318) {
        warn.accept(protocolWarning("http/protobuf", "grpc", endpoint, protocolKey, endpointKey));
        return false;
      }
    }
    return true;
  }

  private static String protocolWarning(
      String endpointProtocol,
      String configuredProtocol,
      String endpoint,
      String protocolKey,
      String endpointKey) {
    return "The value for "
        + endpointKey
        + " ("
        + endpoint
        + ") appears to be a "
        + endpointProtocol
        + " endpoint (port "
        + getPort(endpointProtocol)
        + " is the default port for OTLP with "
        + endpointProtocol
        + " protocol) but value for "
        + protocolKey
        + " is "
        + configuredProtocol
        + ". This is likely to be a configuration error. You may need to change the endpoint port to "
        + getPort(configuredProtocol)
        + " (default port for OTLP with "
        + configuredProtocol
        + " protocol) or change the protocol to "
        + endpointProtocol
        + ".";
  }

  private static String getPort(String protocol) {
    return "http/protobuf".equals(protocol) ? "4318" : "4317";
  }

  private void verifySignalEndpointProtocol(
      String signal, String defaultProtocol, String defaultEndpoint) {
    String protocolKey = "otel.exporter.otlp." + signal + ".protocol";
    String endpointKey = "otel.exporter.otlp." + signal + ".endpoint";
    String protocol = getString(protocolKey, defaultProtocol);
    String endpoint =
        getString(endpointKey, addSignalEndpointSuffix(defaultEndpoint, signal, protocol));
    verifyEndpointProtocol(protocol, endpoint, protocolKey, endpointKey);
  }

  private static String addSignalEndpointSuffix(String endpoint, String signal, String protocol) {
    if ("http/protobuf".equals(protocol)) {
      return endpoint + "/v1/" + signal;
    }
    return endpoint;
  }

  private int getEndpointPort(String endpoint) {
    if (endpoint != null) {
      try {
        URL endpointUrl = new URL(endpoint);
        return endpointUrl.getPort();
      } catch (MalformedURLException ignore) {
        // ignore
      }
    }
    return -1;
  }

  private String getString(String key, String defaultValue) {
    String value = customized.get(key);
    if (value == null) {
      value = config.getString(key);
    }
    return value == null ? defaultValue : value;
  }
}
