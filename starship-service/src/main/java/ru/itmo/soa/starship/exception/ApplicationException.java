package ru.itmo.soa.starship.exception;

import lombok.Getter;
import ru.itmo.soa.common.dto.ErrorResponse;

@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorResponse errorResponse;

    public ApplicationException(ErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }
}
