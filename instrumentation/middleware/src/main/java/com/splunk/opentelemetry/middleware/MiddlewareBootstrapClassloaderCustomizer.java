package com.splunk.opentelemetry.middleware;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.shared.MiddlewareHolder;
import io.opentelemetry.javaagent.spi.BootstrapPackagesProvider;
import java.util.Collections;
import java.util.List;

@AutoService(BootstrapPackagesProvider.class)
public class MiddlewareBootstrapClassloaderCustomizer implements BootstrapPackagesProvider {

  @Override
  public List<String> getPackagePrefixes() {
    return Collections.singletonList(MiddlewareHolder.class.getPackageName());
  }
}
