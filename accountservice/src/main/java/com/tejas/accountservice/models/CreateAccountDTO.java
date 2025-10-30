package com.tejas.accountservice.models;

import lombok.Data;

@Data
public class CreateAccountDTO {
	private int userId;
	private String accounttype;
}
