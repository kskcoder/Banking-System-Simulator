package com.tejas.bankauthservice.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Change password request")
public class ChangePasswordRequest {
	@NotNull
	@Schema(description = "Current password", example = "OldPass123!", required = true)
	private String oldPassword;
	
	@NotNull
	@Schema(description = "New password", example = "NewPass123!", required = true)
	private String newPassword;
}
