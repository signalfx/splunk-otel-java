package com.splunk.opentelemetry;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JettySmokeTest extends AppServerTest {

  private static Stream<Arguments> supportedConfigurations() {
    return Stream.of(
        arguments(new JettyConfiguration("jetty:9.4-jre11-slim", new ExpectedServerAttributes("HandlerCollection.handle", "jetty", "9.4.35.v20201120"))),
        arguments(new JettyConfiguration("jetty:10.0.0.beta3-jdk11-slim", new ExpectedServerAttributes("HandlerList.handle", "jetty", "10.0.0.beta3")))
    );
  }

  @ParameterizedTest
  @MethodSource("supportedConfigurations")
  void jetty_smoke_test(JettyConfiguration config) throws IOException, InterruptedException {
    startTarget(config.imageName);

    assertServerHandler(config.serverAttributes);
  }

  static class JettyConfiguration {
    final String imageName;
    final ExpectedServerAttributes serverAttributes;

    public JettyConfiguration(String imageName, ExpectedServerAttributes serverAttributes) {
      this.imageName = imageName;
      this.serverAttributes = serverAttributes;
    }

    @Override
    public String toString() {
      return serverAttributes.middlewareName + ": " + serverAttributes.middlewareVersion;
    }
  }
}
