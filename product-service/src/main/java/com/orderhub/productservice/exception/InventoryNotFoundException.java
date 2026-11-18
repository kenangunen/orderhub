package com.orderhub.productservice.exception;

public class InventoryNotFoundException extends RuntimeException {
  public InventoryNotFoundException(String message) {
    super(message);
  }
}
