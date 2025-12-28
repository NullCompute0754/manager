package me.ncexce.manager.pojo.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String username;
    private String password;
    private String role;
}
