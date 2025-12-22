package com.tejas.bankauthservice.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {
	@NotNull
	private String username;
	@NotNull
	private String password;
}
