package com.tejas.bankingcommon.dto;

import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.bankingcommon.enums.TransactionType;

import lombok.Data;

@Data
public class TransactionEvent {
    private Long transactionId;
    private String userId;
    private Long paymentId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double amount;
    private double balanceAfter;
    private TransactionType type;
    private TransactionStatus status; 
}
