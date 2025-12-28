package me.ncexce.manager.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RegisterRequestDTO {
    private String username;
    private String password;
    private String role;

}
