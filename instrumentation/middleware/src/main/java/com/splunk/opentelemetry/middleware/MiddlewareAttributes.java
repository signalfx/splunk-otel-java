package com.splunk.opentelemetry.middleware;

public enum MiddlewareAttributes {
  MIDDLEWARE_NAME("middleware.name"),
  MIDDLEWARE_VERSION("middleware.version");

  public final String key;

  MiddlewareAttributes(String key) {

    this.key = key;
  }
}
