package com.us.unix.restfull.exceptions;

/**
 * HTTP 429 Rate Limit Error exception class.
 */
public class RateLimitError extends HttpResponseException {
  /**
   * Exception class.
   *
   * @param message Error message.
   */
  public RateLimitError(String message) {
    super();
  }
}
