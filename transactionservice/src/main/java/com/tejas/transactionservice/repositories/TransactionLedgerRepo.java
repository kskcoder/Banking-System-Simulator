package com.tejas.transactionservice.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejas.transactionservice.models.TransactionLedgerRecord;

public interface TransactionLedgerRepo extends JpaRepository<TransactionLedgerRecord, Long> {
	public abstract TransactionLedgerRecord getByParentTransactionIdAndAccountNumber(long transactionId, String accountNumber);
	
	public abstract List<TransactionLedgerRecord> getByAccountNumberAndType(String accountNumber, String type);

	public abstract List<TransactionLedgerRecord> getByAccountNumber(String accountNumber);
}