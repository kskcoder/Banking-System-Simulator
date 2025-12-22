package com.tejas.bankaccountservice.models;


import com.tejas.bankingcommon.enums.AccountType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountDTO {
	@NotNull
	private int userId;
	@NotNull
	private double initialAmount;
	@NotNull
	private AccountType accountType;
}
