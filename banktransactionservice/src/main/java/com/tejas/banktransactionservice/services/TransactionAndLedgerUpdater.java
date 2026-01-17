package com.tejas.banktransactionservice.services;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionType;
import com.tejas.banktransactionservice.models.Transaction;
import com.tejas.banktransactionservice.models.TransactionLedgerRecord;
import com.tejas.banktransactionservice.repositories.TransactionLedgerRepo;
import com.tejas.banktransactionservice.repositories.TransactionRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionAndLedgerUpdater {
	private final TransactionRepo repo;
	
	private final TransactionLedgerRepo ledgerRepo;
	
	@Autowired
	private CacheManager cacheManager;
	
	private void evictTransactionCache(Long transactionId) {
		Cache transactionCache = cacheManager.getCache("transaction_details");
		if (transactionCache != null && transactionId != null) {
			transactionCache.evict(transactionId);
		}
	}
	
	private void evictTransactionRecordsCache(String accountNumber) {
		Cache transactionRecordsCache = cacheManager.getCache("transaction_records");
		if (transactionRecordsCache != null) {
			transactionRecordsCache.clear();
		}
		
		Cache transactionRecordsDatedCache = cacheManager.getCache("transaction_records_dated");
		if (transactionRecordsDatedCache != null) {
			transactionRecordsDatedCache.clear();
		}
	}
	
	private void evictStatementCache(String accountNumber) {
		Cache statementCache = cacheManager.getCache("statement_pdf");
		if (statementCache != null) {
			statementCache.clear();
		}
	}
	
	private void evictAllLedgerTransactionsCache() {
		Cache allLedgerCache = cacheManager.getCache("all_ledger_transactions");
		if (allLedgerCache != null) {
			allLedgerCache.clear();
		}
	}
	
	private void evictAllTransfersCache() {
		Cache allTransfersCache = cacheManager.getCache("all_transfers");
		if (allTransfersCache != null) {
			allTransfersCache.clear();
		}
	}
	
	public void saveTransaction(TransactionEvent trEvent) {
		if (trEvent.getTransactionId() == null) {
			return;
		}
		Transaction tx = repo.findById(trEvent.getTransactionId())
			.orElseThrow(() -> new com.tejas.bankingcommon.exceptions.NotFoundException("Transaction not found"));
		tx.setStatus(trEvent.getStatus());
		tx.setUpdatedAt(LocalDateTime.now());
		Transaction savedTx = repo.save(tx);
		evictTransactionCache(savedTx.getId());
		evictTransactionRecordsCache(savedTx.getFromAccount());
		evictTransactionRecordsCache(savedTx.getToAccount());
		evictStatementCache(savedTx.getFromAccount());
		evictStatementCache(savedTx.getToAccount());
		evictAllTransfersCache();
		evictAllLedgerTransactionsCache();
	}
	
	public void saveTransactionRecord(TransactionEvent trEvent, boolean isRepayOrCredit) {
		TransactionLedgerRecord rec = ledgerRepo.getByParentTransactionIdAndAccountNumber(trEvent.getTransactionId(), 
				isRepayOrCredit ? trEvent.getToAccountNumber() : trEvent.getFromAccountNumber());
		
		if (rec != null) {
			rec.setBalanceAfter(trEvent.getBalanceAfter());
			rec.setStatus(trEvent.getStatus());
		} else {
			rec = createNewLedgerRecord(trEvent, isRepayOrCredit);
		}
		
		ledgerRepo.save(rec);
		String accountNumber = isRepayOrCredit ? trEvent.getToAccountNumber() : trEvent.getFromAccountNumber();
		evictTransactionRecordsCache(accountNumber);
		evictStatementCache(accountNumber);
		evictAllLedgerTransactionsCache();
	}
	
	public TransactionLedgerRecord createNewLedgerRecord(TransactionEvent trEvent, boolean isRepayOrCredit) {
		Transaction tr = repo.findById(trEvent.getTransactionId())
			.orElseThrow(() -> new com.tejas.bankingcommon.exceptions.NotFoundException("Transaction not found"));
		TransactionLedgerRecord rec = new TransactionLedgerRecord();
		rec.setParentTransactionId(tr.getId());
		rec.setAccountNumber(isRepayOrCredit ? tr.getToAccount() : tr.getFromAccount());
		rec.setCounterparty(isRepayOrCredit ? tr.getFromAccount() : tr.getToAccount());
		rec.setType(isRepayOrCredit ? TransactionType.CREDIT : TransactionType.DEBIT);     
		rec.setAmount(tr.getAmount());
		rec.setBalanceAfter(trEvent.getBalanceAfter());
		rec.setStatus(tr.getStatus());
		rec.setCreatedAt(tr.getCreatedAt()); 
		
		return rec;
	}

	public void saveInterestTransaction(TransactionEvent trEvent) {
		TransactionLedgerRecord rec = new TransactionLedgerRecord();
		rec.setParentTransactionId(null);
		rec.setAccountNumber(trEvent.getToAccountNumber());
		rec.setCounterparty(trEvent.getFromAccountNumber());
		rec.setType(trEvent.getType());     
		rec.setAmount(trEvent.getAmount());
		rec.setBalanceAfter(trEvent.getBalanceAfter());
		rec.setStatus(trEvent.getStatus());
		rec.setCreatedAt(LocalDateTime.now()); 
		
		ledgerRepo.save(rec);
		evictTransactionRecordsCache(trEvent.getToAccountNumber());
		evictStatementCache(trEvent.getToAccountNumber());
		evictAllLedgerTransactionsCache();
	}

	
}
