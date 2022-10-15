package com.splunk.opentelemetry.servicename;

/**
 * Functional interface for implementations that know how to detect
 * a service name for a specific application server type.
 */
public interface ServiceNameDetector {
  String detect() throws Exception;
}
