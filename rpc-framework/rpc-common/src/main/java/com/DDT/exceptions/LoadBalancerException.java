package com.DDT.exceptions;

public class LoadBalancerException extends RuntimeException {
  public LoadBalancerException() {
    super();
  }

  public LoadBalancerException(String message) {
    super(message);
  }
}
