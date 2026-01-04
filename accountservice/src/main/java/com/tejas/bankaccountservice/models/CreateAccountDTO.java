package com.tejas.bankaccountservice.models;

import com.tejas.bankingcommon.enums.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Create account request")
public class CreateAccountDTO {
	@NotNull
	@Schema(description = "User ID for the account", example = "1", required = true)
	private int userId;
	
	@NotNull
	@Schema(description = "Initial deposit amount", example = "1000.0", required = true)
	private double initialAmount;
	
	@NotNull
	@Schema(description = "Type of account", example = "SAVINGS", required = true)
	private AccountType accountType;
}
