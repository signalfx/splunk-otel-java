package com.splunk.opentelemetry.servicename;

import javax.annotation.Nullable;
import java.nio.file.Path;

public interface AppServer {
  /** Path to directory to be scanned for deployments. */
  Path getDeploymentDir() throws Exception;

  Class<?> getServerClass();

  default boolean supportsEar(){
    return false;
  };

  /** Use to ignore default applications that are bundled with the app server. */
  default boolean isValidAppName(Path path) {
    return true;
  }

  /** Use to ignore default applications that are bundled with the app server. */
  default boolean isValidResult(Path path, @Nullable String result) {
    return true;
  }




}
