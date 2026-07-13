package com.splunk.hackity.hack.control;

import java.util.logging.Logger;

public class CommandDispatcherImpl implements CommandDispatcher {

  public static final Logger LOGGER = Logger.getLogger(CommandDispatcherImpl.class.getName());

  private final BigDumper threadDumper;

  public CommandDispatcherImpl(BigDumper threadDumper) {this.threadDumper = threadDumper;}

  @Override
  public void dispatch(String contentType, String body) {
    String[] parts = body.split("\n");
    if(parts.length < 1) {
      LOGGER.warning("Missing useful command body.");
      return;
    }
    String command = parts[0].trim();
    switch (command) {
      case "thread.dump":
        threadDumper.dump();
        break;
      default:
        LOGGER.warning("Unknown command: " + command);
    }
  }
}
