package com.splunk.opentelemetry;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class JettySmokeTest extends AppServerTest {

  @Test
  void jetty_smoke_test() throws IOException, InterruptedException {
    startTarget("jetty:9.4-jre11-slim");

    assertServerHandler(new ExpectedServerAttributes("HandlerCollection.handle", "jetty", "9.4"));
  }
}
