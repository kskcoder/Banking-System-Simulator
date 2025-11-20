package com.tejas.accountservice.models;

import com.tejas.accountservice.enums.AccountType;

import lombok.Data;

@Data
public class CreateAccountDTO {
	private int userId;
	private double initialAmount;
	private AccountType accountType;
}
