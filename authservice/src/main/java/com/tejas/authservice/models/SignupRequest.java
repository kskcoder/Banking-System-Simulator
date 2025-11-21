package com.tejas.authservice.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignupRequest {
	@NotNull
	private String username;
	@NotNull
	private String email;
	@NotNull
	private String password;
	@NotNull
	private String phone; 	
}
