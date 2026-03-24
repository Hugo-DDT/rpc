package com.DDT.exceptions;

public class NetworkException extends RuntimeException {
  public NetworkException() {
  }

  public NetworkException(String message) {
    super(message);
  }

  public NetworkException(Throwable cause) {
    super(cause);
  }
}
