package com.tejas.transactionservice.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequest {
	@NotNull
	private String fromAccount;
	@NotNull
    private String toAccount;
	@NotNull
    private double amount;
}