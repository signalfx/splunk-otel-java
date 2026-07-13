package com.splunk.hackity.hack.control;

public class NoOpCommandDispatcher implements CommandDispatcher {
  @Override
  public void dispatch(String contentType, String body) {
    // nop
  }
}
