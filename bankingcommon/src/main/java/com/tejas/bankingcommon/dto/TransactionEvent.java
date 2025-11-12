package com.tejas.bankingcommon.dto;

import lombok.Data;

@Data
public class TransactionEvent {
    private long transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double amount;
    private String type;
    private String status; 
}
