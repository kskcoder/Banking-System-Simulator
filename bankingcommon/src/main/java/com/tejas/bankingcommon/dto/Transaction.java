package com.tejas.bankingcommon.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Transaction {
	private long id;
	private String fromAccount;
	private String toAccount;
	private double amount;
	private String status;
	private LocalDateTime createdAt; 
	
	private LocalDateTime updatedAt;
}
