package me.ncexce.manager.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponseDTO {
    private String message;
    private int statusCode;
}
