package com.tejas.transactionservice.models;

import java.time.LocalDateTime;

import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.bankingcommon.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name="transaction_ledger")
public class TransactionLedgerRecord {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	
	private Long parentTransactionId;
	private String accountNumber;      
    private String counterparty;   

    @Column(name="type")
    @Enumerated(EnumType.STRING)
    private TransactionType type;           

    private double amount;
    private double balanceAfter;

    @Enumerated(EnumType.STRING)
	private TransactionStatus status;

    @Column(name="created_at")
    private LocalDateTime createdAt; 
}
