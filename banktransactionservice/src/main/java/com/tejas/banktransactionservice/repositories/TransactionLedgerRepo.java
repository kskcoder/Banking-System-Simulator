package com.tejas.banktransactionservice.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tejas.bankingcommon.enums.TransactionType;
import com.tejas.banktransactionservice.models.TransactionLedgerRecord;

public interface TransactionLedgerRepo extends JpaRepository<TransactionLedgerRecord, Long> {
	public abstract TransactionLedgerRecord getByParentTransactionIdAndAccountNumber(long transactionId, String accountNumber);
	
	public abstract Page<TransactionLedgerRecord> getByAccountNumberAndType(String accountNumber, TransactionType type, Pageable pageable);

	public abstract Page<TransactionLedgerRecord> getByAccountNumberAndTypeAndCreatedAtBetween(String accountNum, TransactionType type,
			LocalDateTime from, LocalDateTime to, Pageable pageable);
	
	public abstract Page<TransactionLedgerRecord> getByAccountNumber(String accountNumber, Pageable pageable);

	public abstract Page<TransactionLedgerRecord> getByAccountNumberAndCreatedAtBetween(String accountNum,
			LocalDateTime from, LocalDateTime to, Pageable pageable);
	
	public abstract List<TransactionLedgerRecord> getByAccountNumberAndCreatedAtBetween(String accountNum,
			LocalDateTime from, LocalDateTime to);
}