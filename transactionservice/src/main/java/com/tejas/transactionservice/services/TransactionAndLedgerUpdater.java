package com.tejas.transactionservice.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.transactionservice.enums.TransactionType;
import com.tejas.transactionservice.models.Transaction;
import com.tejas.transactionservice.models.TransactionLedgerRecord;
import com.tejas.transactionservice.repositories.TransactionLedgerRepo;
import com.tejas.transactionservice.repositories.TransactionRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionAndLedgerUpdater {
	private final TransactionRepo repo;
	
	private final TransactionLedgerRepo ledgerRepo;
	
	public void saveTransaction(TransactionEvent trEvent) {
		Transaction tx = repo.findById(trEvent.getTransactionId());
		tx.setStatus(trEvent.getStatus());
		tx.setUpdatedAt(LocalDateTime.now());
		repo.save(tx);		
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
	}
	
	public TransactionLedgerRecord createNewLedgerRecord(TransactionEvent trEvent, boolean isRepayOrCredit) {
		Transaction tr = repo.findById(trEvent.getTransactionId());
		TransactionLedgerRecord rec = new TransactionLedgerRecord();
		rec.setParentTransactionId(tr.getId());
		rec.setAccountNumber(isRepayOrCredit ? tr.getToAccount() : tr.getFromAccount());
		rec.setCounterparty(isRepayOrCredit ? tr.getFromAccount() : tr.getToAccount());
		rec.setType(isRepayOrCredit ? TransactionType.CREDIT.toString() : TransactionType.DEBIT.toString());     
		rec.setAmount(tr.getAmount());
		rec.setBalanceAfter(isRepayOrCredit ? trEvent.getBalanceAfter() : 0.0);
		rec.setStatus(tr.getStatus());
		rec.setCreatedAt(tr.getCreatedAt()); 
		
		return rec;
	}

	
}
