package com.splunk.opentelemetry.javaagent.shared;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiddlewareHolder {

  private static final Logger log = LoggerFactory.getLogger(MiddlewareHolder.class);

  public static final AtomicReference<String> middlewareName = new AtomicReference<>();
  public static final AtomicReference<String> middlewareVersion = new AtomicReference<>();

  public static void trySetName(String name) {
    if (!middlewareName.compareAndSet(null, name)) {
      log.debug("Trying to re-set middleware name from {} to {}", middlewareName.get(), name);
    }
  }

  public static void trySetVersion(String version) {
    if (!middlewareVersion.compareAndSet(null, version)) {
      log.debug("Trying to re-set middleware version from {} to {}", middlewareVersion.get(), version);
    }
  }
}
