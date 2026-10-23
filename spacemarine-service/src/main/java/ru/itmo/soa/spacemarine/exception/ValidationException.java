package ru.itmo.soa.spacemarine.exception;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
    private final int code;

    public ValidationException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ValidationException(String message) {
        this(422, message);
    }
}
