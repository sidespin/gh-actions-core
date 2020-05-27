package com.github.sidespin.actions.core;

import static com.github.sidespin.actions.core.Command.issue;
import static com.github.sidespin.actions.core.Command.issueCommand;
import static com.github.sidespin.actions.core.Command.toCommandValue;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.github.sidespin.actions.core.Command.CommandProperties;

public class Action {
  
  private PrintStream out;
  
  private Map<String, String> env;

  private ExitCode exitCode = ExitCode.Success;

  public int getExitCode() {
    return exitCode.getCode();
  }

  /**
   * Interface for getInput options
   */
//	export interface InputOptions {
//	  /** Optional. Whether the input is required. If required and not present, will throw. Defaults to false */
//	  required?: boolean
//	}

  public Action(PrintStream out, Map<String, String> env) {
    this.out = out;
    this.env = env == null? new HashMap<>(System.getenv()):env ;
  }
  
  public Action(){
    this(System.out, new HashMap<>(System.getenv()));
  }


  //-----------------------------------------------------------------------
  // Variables
  //-----------------------------------------------------------------------

  /**
   * Sets env variable for this action and future actions in the job
   * @param name the name of the variable to set
   * @param val the value of the variable. Non-string values will be converted to a string via JSON.stringify
   */
  public void exportVariable(String name, Object val) {
    var convertedVal = toCommandValue(val);
    env.put(name, convertedVal);
    issueCommand(out, "set-env", new CommandProperties(name), convertedVal);
  }

  /**
   * Registers a secret which will get masked from logs
   * @param secret value of the secret
   */
  public void setSecret(String secret ) {
    issueCommand(out, "add-mask", null, secret);
  }

  /**
   * Prepends inputPath to the PATH (for this action and future actions)
   * @param inputPath
   */
  public void addPath(String inputPath) {
    issueCommand(out, "add-path", null, inputPath);
    String PATH = env.getOrDefault("PATH", "");
    env.put("PATH", inputPath+ File.pathSeparator + PATH);
  }

  /**
   * Gets the value of an input.  The value is also trimmed.
   *
   * @param     name     name of the input to get
   * @returns   string
   */
  public String getInput(String name) {
    return getInput(name, false);
  }
  
  public String getInput(String name, boolean required) {
    String val = env.getOrDefault("INPUT_"+name.replaceAll(" ","_").toUpperCase(), "");
    if (required && val.isEmpty()) {
        throw new RuntimeException("Input required and not supplied: "+name);
    }

    return val.trim();
  }

  /**
   * Sets the value of an output.
   *
   * @param     name     name of the output to set
   * @param     value    value to store. Non-string values will be converted to a string via JSON.stringify
   */
  public void setOutput(String name, Object value) {
    issueCommand(out, "set-output", new CommandProperties(name), value);
  }

  /**
   * Enables or disables the echoing of commands into stdout for the rest of the step.
   * Echoing is disabled by default if ACTIONS_STEP_DEBUG is not set.
   *
   */
  public void setCommandEcho(boolean enabled) {
    issue(out, "echo", enabled ? "on" : "off");
  }

  //-----------------------------------------------------------------------
  // Results
  //-----------------------------------------------------------------------

  /**
   * Sets the action status to failed.
   * When the action exits it will be with an exit code of 1
   * @param message add error issue message
   */
  public void setFailed(String message) {
    this.exitCode = ExitCode.Failure;
    error(message);
  }
  
  /**
   * Sets the action status to failed.
   * When the action exits it will be with an exit code of 1
   * @param error add error
   */
  public void setFailed(Throwable error) {
    this.exitCode = ExitCode.Failure;
    error(error);
  }

  //-----------------------------------------------------------------------
  // Logging Commands
  //-----------------------------------------------------------------------

  /**
   * Gets whether Actions Step Debug is on or not
   */
  public boolean isDebug(){
    return "1".equals(env.get("RUNNER_DEBUG"));
  }

  /**
   * Writes debug message to user log
   * @param message debug message
   */
  public void debug(String message ) {
    issueCommand(out, "debug", null, message);
  }

  /**
   * Adds an error issue
   * @param message error issue message.
   */
  public void error(String message) {
    issue(out, "error", message);
  }
  
  /**
   * Adds an error issue
   * @param message error issue message. Errors will be converted to string via getMessage()
   */
  public void error(Throwable error) {
    error(getMessage(error));
  }

  private String getMessage(Throwable error) {
    Throwable rootCause = getCause(error);
    String message = rootCause.getMessage();
    if (message==null || message.isEmpty()) {
      StackTraceElement[] stackTrace = rootCause.getStackTrace();
      message = rootCause.getClass().getName()+" @ "+stackTrace[0].toString();
    }
    return message;
  }

  /**
   * Adds a warning issue
   * @param message warning issue message. Errors will be converted to string via toString()
   */
  public void warning(String message) {
    issue(out, "warning", message);
  }

  /**
   * Adds a warning issue
   * @param message warning issue message. Errors will be converted to string via getMessage()
   */
  public void warning(Throwable error) {
    warning(getMessage(error));
  }
  
  /**
   * Writes info to log with out.println.
   * @param message info message
   */
  public void info(String message) {
    out.println(message);
  }

  /**
   * Begin an output group.
   *
   * Output until the next `groupEnd` will be foldable in this group
   *
   * @param name The name of the output group
   */
  public void startGroup(String name) {
    issue(out, "group", name);
  }

  /**
   * End an output group.
   */
  public void endGroup() {
    issue(out, "endgroup");
  }

  /**
   * Wrap an asynchronous function call in a group.
   *
   * Returns the same type as the function itself.
   *
   * @param name The name of the group
   * @param fn The function to wrap in the group
   */
//	public CompletableFuture<T> group<T>(String name: , fn: () => Promise<T>):  {
//	  startGroup(name);
//
//	  let result: T
//
//	  try {
//	    result = await fn()
//	  } finally {
//	    endGroup();
//	  }
//
//	  return result;
//	}

  //-----------------------------------------------------------------------
  // Wrapper action state
  //-----------------------------------------------------------------------

  /**
   * Saves state for current action, the state can only be retrieved by this action's post job execution.
   *
   * @param     name     name of the state to store
   * @param     value    value to store. Non-string values will be converted to a string via JSON.stringify
   */
  public void saveState(String name, Object value) {
    issueCommand(out, "save-state", new CommandProperties(name), value);
  }

  /**
   * Gets the value of an state set by this action's main execution.
   *
   * @param     name     name of the state to get
   * @returns   string
   */
  public String getState(String name) {
    return env.getOrDefault("STATE_"+name, "");
  }
  
  private Throwable getCause(Throwable t) {
    Throwable rootCause = t;
      while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
          rootCause = rootCause.getCause();
      }
      return rootCause;
  }
}
