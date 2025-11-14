package com.tejas.transactionservice.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.transactionservice.models.Transaction;
import com.tejas.transactionservice.repositories.TransactionRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusUpdater {
	private final TransactionRepo repo; 
	
	public void saveTransaction(TransactionEvent trEvent) {
		Transaction tx = repo.findById(trEvent.getTransactionId());
		tx.setStatus(trEvent.getStatus());
		tx.setUpdatedAt(LocalDateTime.now());
		repo.save(tx);		
	}
}
