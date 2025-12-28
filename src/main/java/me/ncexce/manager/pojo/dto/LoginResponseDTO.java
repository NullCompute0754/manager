package me.ncexce.manager.pojo.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String token;
    private String username;
    private String role;
    private int statusCode;
}
