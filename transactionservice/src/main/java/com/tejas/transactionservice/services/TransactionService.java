package com.tejas.transactionservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.transactionservice.models.Transaction;
import com.tejas.transactionservice.models.TransferRequest;
import com.tejas.transactionservice.repositories.TransactionRepo;
import com.tejas.transactionservice.services.enums.TransactionType;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepo repo;
    
    @Autowired
    private TransactionProducer trProducer;

    @Transactional
    public ResponseEntity<Transaction> transfer(TransferRequest request) {
        Transaction txn = new Transaction();
        txn.setFromAccount(request.getFromAccount());
        txn.setToAccount(request.getToAccount());
        txn.setAmount(request.getAmount());
        txn.setStatus("PENDING");
        
        if (request.getAmount() <= 0.0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        } else if (request.getFromAccount().equals(request.getToAccount())) {
        	throw new IllegalArgumentException("Sender and receiver must be different");
        }
        repo.save(txn);
        
        TransactionEvent event = new TransactionEvent(); 
    	event.setTransactionId(txn.getId());
    	event.setFromAccountNumber(txn.getFromAccount());
    	event.setToAccountNumber(txn.getToAccount());
    	event.setAmount(txn.getAmount());
    	event.setType(TransactionType.DEBIT.toString());
    	event.setStatus(TransactionStatus.PENDING.toString());  
        
        trProducer.dispatchDebitWithRetry(event);
        
        return ResponseEntity.ok(txn);
    }

    @Transactional
	public void saveTransaction(TransactionEvent trEvent) {
		Transaction tx = repo.findById(trEvent.getTransactionId());
		tx.setStatus(trEvent.getStatus());
		
		repo.save(tx);
	}
}

