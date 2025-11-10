package com.tejas.bankingcommon.dto;

import lombok.Data;

@Data
public class TransactionEvent {
    private String transactionId;
    private String accountNumber;
    private double amount;
    private String type;
    private String status; 
}
