package com.tejas.accountservice.models;

import lombok.Data;

@Data
public class TransferRequest {
	private String fromAccount;
    private String toAccount;
    private double amount;
}
