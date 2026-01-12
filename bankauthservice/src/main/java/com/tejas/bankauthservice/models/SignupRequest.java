package com.tejas.bankauthservice.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "User signup request")
public class SignupRequest {
	@NotNull
	@Schema(description = "Username for the new account", example = "john_doe", required = true)
	private String username;
	
	@NotNull
	@Schema(description = "Email address", example = "john.doe@example.com", required = true)
	private String email;
	
	@NotNull
	@Schema(description = "Password for the account", example = "SecurePass123!", required = true)
	private String password;
	
	@NotNull
	@Schema(description = "Phone number", example = "1234567890", required = true)
	private String phone; 	
}
