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
public class GeneralCardResponse {
	private String maskedCardNumber;
	private String expiry;
	private AccountCardStatus status;
	private double limit;
}
