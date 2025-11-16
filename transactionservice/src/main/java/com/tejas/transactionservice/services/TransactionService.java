package com.tejas.transactionservice.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tejas.bankingcommon.dto.TransactionEvent;
import com.tejas.bankingcommon.enums.TransactionStatus;
import com.tejas.transactionservice.enums.TransactionType;
import com.tejas.transactionservice.feign.AccountInterface;
import com.tejas.transactionservice.models.Transaction;
import com.tejas.transactionservice.models.TransferRequest;
import com.tejas.transactionservice.repositories.TransactionRepo;

import feign.FeignException;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepo repo;
    
    @Autowired
    private TransactionProducer trProducer;
    
	@Autowired
	private AccountInterface accInterface;

    @Transactional
    public ResponseEntity<Transaction> transfer(TransferRequest request) {
        Transaction txn = new Transaction();
        txn.setFromAccount(request.getFromAccount());
        txn.setToAccount(request.getToAccount());
        txn.setAmount(request.getAmount());
        txn.setStatus("PENDING");
        txn.setCreatedAt(LocalDateTime.now());
        txn.setUpdatedAt(LocalDateTime.now());
        
        if (request.getAmount() <= 0.0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        } else if (request.getFromAccount().equals(request.getToAccount())) {
        	throw new IllegalArgumentException("Sender and receiver must be different");
        }
        repo.save(txn);
        
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        TransactionEvent event = new TransactionEvent(); 
    	event.setTransactionId(txn.getId());
    	event.setFromAccountNumber(txn.getFromAccount());
    	event.setToAccountNumber(txn.getToAccount());
    	event.setAmount(txn.getAmount());
    	event.setUserId(Integer.parseInt(userId));
    	event.setType(TransactionType.DEBIT.toString());
    	event.setStatus(TransactionStatus.PENDING.toString());  
        
        trProducer.dispatchDebitWithRetry(event);
        
        return ResponseEntity.ok(txn);
    }

    @Transactional
	public void saveTransaction(TransactionEvent trEvent) {
		Transaction tx = repo.findById(trEvent.getTransactionId());
		tx.setStatus(trEvent.getStatus());
		tx.setUpdatedAt(LocalDateTime.now());
		repo.save(tx);
	}

	public ResponseEntity<Transaction> getTransaction(int txnId) {
		Transaction tx = repo.findById(txnId);
		
		if (tx != null) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<List<Transaction>> getDebitTransaction(String accountNum) {
		if (!isOwner(accountNum)) {return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);}
		
		List<Transaction> tx = repo.findByFromAccount(accountNum);
		
		if (!tx.isEmpty()) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<List<Transaction>> getCreditTransaction(String accountNum) {
		if (!isOwner(accountNum)) {return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);}
		
		List<Transaction> tx = repo.findByToAccount(accountNum);
		
		if (!tx.isEmpty()) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}

	public ResponseEntity<List<Transaction>> getAllTransaction(String accountNum) {
		if (!isOwner(accountNum)) {return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);}
		
		List<Transaction> tx = repo.findByFromAccount(accountNum);
		tx.addAll(repo.findByToAccount(accountNum));
		
		if (!tx.isEmpty()) {
			return new ResponseEntity<>(tx, HttpStatus.OK);
		}
		
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	private boolean isOwner(String pathAccNo) {
		String role = ((ServletRequestAttributes) RequestContextHolder
		        .getRequestAttributes())
		        .getRequest()
		        .getHeader("X-User-Role");
		try {
			 boolean isOwner = accInterface.isOwnerOfAccount(pathAccNo).getBody();
			 if (!isOwner || !role.equals("ADMIN")) {
				return false;                
	    	}
	    } catch (FeignException e) {
	    	return false;
	    }	
		return true;
	}

}

