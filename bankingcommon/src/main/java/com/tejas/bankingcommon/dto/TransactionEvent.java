package com.tejas.bankingcommon.dto;

import com.tejas.bankingcommon.enums.TransactionType;

import lombok.Data;

@Data
public class TransactionEvent {
    private Long transactionId;
    private Integer userId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double amount;
    private double balanceAfter;
    private TransactionType type;
    private String status; 
}
