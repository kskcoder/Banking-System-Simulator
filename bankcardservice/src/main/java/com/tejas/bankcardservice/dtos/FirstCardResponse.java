package com.tejas.bankcardservice.dtos;

import com.tejas.bankingcommon.enums.AccountCardStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class FirstCardResponse {
	private String cardNumber;
	private String cvv;
	private String expiry;
	private String maskedCardNumber;
	private AccountCardStatus status;
	private double limit;
}
