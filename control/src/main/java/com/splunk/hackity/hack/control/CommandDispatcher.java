package com.splunk.hackity.hack.control;

public interface CommandDispatcher {
  void dispatch(String contentType, String body);
}
