package com.us.unix.restfull.exceptions;

/**
 * HTTP 403 Permission Denied Error exception class.
 */
public class PermissionDeniedError extends HttpResponseException {
  /**
   * Exception class.
   *
   * @param message Error message.
   */
  public PermissionDeniedError(String message) {
    super();
  }
}
