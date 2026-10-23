package ru.itmo.soa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ErrorResponse {
    private Integer code;
    private String message;
    private String time;
}
