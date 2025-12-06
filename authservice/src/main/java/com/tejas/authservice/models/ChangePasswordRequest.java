package com.tejas.authservice.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangePasswordRequest {
	@NotNull
	private String oldPassword;
	@NotNull
	private String newPassword;
}
