package com.github.sidespin.actions.core;

/**
 * The code to exit an action
 */
public enum ExitCode {
  /**
   * A code indicating that the action was successful
   */
  Success(0),
  /**
   * A code indicating that the action was a failure
   */
  Failure(1);

  private int code;

  ExitCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
