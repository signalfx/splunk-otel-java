package com.splunk.opentelemetry.instrumentation.nocode;

public class SampleClass {
  public String getName() {
    return "name";
  }

  public String getDetails() {
    return "details";
  }

  public void throwException(int parameter) {
    throw new UnsupportedOperationException("oh no");
  }

  public void doSomething() {
  }

}
