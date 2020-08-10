package com.signalfx.opentelemetry;

import io.opentelemetry.auto.bootstrap.AgentBootstrap;
import java.lang.instrument.Instrumentation;

public class SplunkAgent {
  public static void premain(final String agentArgs, final Instrumentation inst) {
    agentmain(agentArgs, inst);
  }

  public static void agentmain(final String agentArgs, final Instrumentation inst) {
    System.setProperty("otel.exporter", "splunk");
    AgentBootstrap.agentmain(agentArgs, inst);
  }
}
