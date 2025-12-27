package com.hyperchip.common.exception;

/**
 * Thrown when an attempted cart operation cannot complete due to insufficient product stock.
 */
public class OutOfStockException extends RuntimeException {
    public OutOfStockException() { super(); }
    public OutOfStockException(String message) { super(message); }
}
