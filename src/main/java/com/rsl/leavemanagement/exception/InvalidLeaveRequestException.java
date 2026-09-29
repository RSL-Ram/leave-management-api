package com.rsl.leavemanagement.exception;

/**
 * Thrown when a leave request violates a business rule
 * (as opposed to a simple field validation failure).
 */
public class InvalidLeaveRequestException extends RuntimeException {

    public InvalidLeaveRequestException(String message) {
        super(message);
    }
}
