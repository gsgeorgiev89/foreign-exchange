package com.example.fx.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FxException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public FxException(ErrorCode errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public enum ErrorCode {
        CLIENT_NOT_FOUND,
        BALANCE_NOT_FOUND,
        INSUFFICIENT_FUNDS,
        IDEMPOTENCY_KEY_PROCESSING,
        VALIDATION_ERROR
    }
}