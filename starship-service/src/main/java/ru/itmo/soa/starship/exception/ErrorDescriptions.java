package ru.itmo.soa.starship.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.soa.common.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@RequiredArgsConstructor
public enum ErrorDescriptions {
    HANDLER_NOT_FOUND("Не найден обработчик", 404),
    INVALID_STARSHIP_ID("Идентификатор корабля должен быть положительным числом (> 0)", 400),
    INVALID_MARINE_ID("Идентификатор космодесантника должен быть положительным числом (> 0)", 400),
    MARINE_NOT_FOUND("Космодесантник не найден", 404),
    STARSHIP_NOT_FOUND("Космический корабль не найден", 404),
    SERVICE_UNAVAILABLE("Сервис космодесантников временно недоступен", 500),
    INTERNAL_ERROR("Внутренняя ошибка сервера", 500);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final String message;
    private final Integer status;

    public ErrorResponse applicationError() {
        return ErrorResponse.of(this.status, this.message, LocalDateTime.now().format(DATE_FORMATTER));
    }

    public ErrorResponse applicationError(String customMessage) {
        return ErrorResponse.of(this.status, customMessage, LocalDateTime.now().format(DATE_FORMATTER));
    }

    public ApplicationException exception() {
        return new ApplicationException(applicationError());
    }

    public ApplicationException exception(String customMessage) {
        return new ApplicationException(applicationError(customMessage));
    }
}
