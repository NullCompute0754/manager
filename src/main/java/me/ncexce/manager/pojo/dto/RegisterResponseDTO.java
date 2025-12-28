package me.ncexce.manager.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RegisterResponseDTO {
    private String username;
    private int statusCode;
    private String message;
}
