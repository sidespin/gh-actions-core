package com.github.sidespin.actions.core;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Command {

  private static final String CMD_STRING = "::";

  public static void issueCommand(PrintStream out,
                  String command,
                  CommandProperties properties,
                  Object message ) {
    if (out == null) {
      throw new IllegalArgumentException("PrintStream is required");
    }
    var cmd = new Command(command, properties, message==null?"":message);
    out.println(cmd);
  }
  
  public static void issue(PrintStream out, String name, String message) {
    issueCommand(out, name, null, message);
  }
  
  public static void issue(PrintStream out, String name) {
    issueCommand(out, name, null, "");
  }
  
  @SuppressWarnings("serial")
  public static class CommandProperties extends HashMap<String, Object> {
    CommandProperties(){
      super();
    }
    
    CommandProperties(String name){
      super(1);
      put("name", name);
    }
  }

  private String command;
  private String message;
  private CommandProperties properties;

  public Command(String command, CommandProperties properties, Object message) {
    if (command == null) {
      throw new IllegalArgumentException("missing.command");
    }

    this.command = command;
    this.properties = properties;
    this.message = toCommandValue(message);
  }

  public String toString() {
    StringBuilder cmdStr = new StringBuilder(CMD_STRING).append(this.command);

    if (properties != null && !properties.isEmpty()) {
      cmdStr.append(" ");
      String escapedEntries = properties.entrySet()
                        .stream()
                        .filter(e -> e.getValue() != null)
                        .map(this::escape)
                        .collect(Collectors.joining(","));
      cmdStr.append(escapedEntries);
    }

    cmdStr.append(CMD_STRING).append(escapeData(this.message));
    return cmdStr.toString();
  }

  public String escape(Entry<String, Object> entry) {
    return new StringBuilder(entry.getKey())
          .append("=")
          .append(escapeProperty(entry.getValue()))
          .toString();
  }

  /**
   * Sanitizes an input into a string so it can be passed into issueCommand safely
   * 
   * @param input input to sanitize into a string
   */
  public static String toCommandValue(Object input) {
    if (input == null) {
      return "";
    } else if (input instanceof String) {
      return (String) input;
    }
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(input);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String escapeData(Object s) {
    return toCommandValue(s).replaceAll("%", "%25")
                .replaceAll("\r", "%0D")
                .replaceAll("\n", "%0A");
  }

  public static String escapeProperty(Object s) {
    return escapeData(s).replaceAll(":", "%3A")
                        .replaceAll(",", "%2C");
  }

}
