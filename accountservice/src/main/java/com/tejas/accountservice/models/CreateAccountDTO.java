package com.tejas.accountservice.models;


import jakarta.validation.constraints.NotNull;

import com.tejas.accountservice.enums.AccountType;

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
