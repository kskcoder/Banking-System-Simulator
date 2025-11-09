package com.tejas.transactionservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tejas.transactionservice.feign.AccountInterface;
import com.tejas.transactionservice.models.Transaction;
import com.tejas.transactionservice.models.TransferRequest;
import com.tejas.transactionservice.repositories.TransactionRepo;

import feign.FeignException;

@Service
public class TransactionService {

    @Autowired
    private AccountInterface accountInterface;

    @Autowired
    private TransactionRepo repo;

    @Transactional
    public ResponseEntity<Transaction> transfer(TransferRequest request) {
        Transaction txn = new Transaction();
        txn.setFromAccount(request.getFromAccount());
        txn.setToAccount(request.getToAccount());
        txn.setAmount(request.getAmount());
        txn.setStatus("PENDING");
        repo.save(txn);

        try {
            accountInterface.debitAccount(request);
        } catch (FeignException e) {
        	txn.setStatus(e.contentUTF8());
            repo.save(txn);
            
            HttpStatus status = HttpStatus.resolve(e.status());
            if (status == null) {
            	status = HttpStatus.INTERNAL_SERVER_ERROR; 
            }
            return new ResponseEntity<>(txn, status);
        }
        
        try {
            accountInterface.creditAccount(request);
        } catch (FeignException e) {
        	txn.setStatus(e.contentUTF8());
        	repo.save(txn);
             
            TransferRequest tf = new TransferRequest();
            tf.setToAccount(request.getFromAccount());
            tf.setAmount(request.getAmount());
            
            accountInterface.creditAccount(tf);
            HttpStatus status = HttpStatus.resolve(e.status());
            if (status == null) {
            	status = HttpStatus.INTERNAL_SERVER_ERROR; 
            }
            return new ResponseEntity<>(txn, status);
        }
        
        txn.setStatus("SUCCESS");
        repo.save(txn);
        return ResponseEntity.ok(txn);

        
    }
}

