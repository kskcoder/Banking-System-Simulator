package com.tejas.bankauthservice.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "User login request")
public class LoginRequest {
	@NotNull
	@Schema(description = "Username", example = "john_doe", required = true)
	private String username;
	
	@NotNull
	@Schema(description = "Password", example = "SecurePass123!", required = true)
	private String password;
}
