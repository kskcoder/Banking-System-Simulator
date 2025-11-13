package com.tejas.bankingcommon.dto;

import lombok.Data;

@Data
public class TransactionEvent {
    private long transactionId;
    private int userId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double amount;
    private String type;
    private String status; 
}
