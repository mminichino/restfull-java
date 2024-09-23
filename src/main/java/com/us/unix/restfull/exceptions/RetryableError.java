package com.us.unix.restfull.exceptions;

/**
 * Retryable Error exception class.
 */
public class RetryableError extends HttpResponseException {
  /**
   * Exception class.
   *
   * @param message Error message.
   */
  public RetryableError(String message) {
    super();
  }
}
