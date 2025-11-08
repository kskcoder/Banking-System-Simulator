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
            ResponseEntity<String> debitResponse = accountInterface.debitAccount(request);
            if (!debitResponse.getStatusCode().is2xxSuccessful()) {
                txn.setStatus("FAILED_DEBIT");
                repo.save(txn);
                return new ResponseEntity<>(txn, HttpStatus.BAD_REQUEST);
            }

            ResponseEntity<String> creditResponse = accountInterface.creditAccount(request);

            if (!creditResponse.getStatusCode().is2xxSuccessful()) {
                txn.setStatus("FAILED_CREDIT");
                repo.save(txn);
                 
                TransferRequest tf = new TransferRequest();
                tf.setToAccount(request.getFromAccount());
                tf.setAmount(request.getAmount());
                
                accountInterface.creditAccount(tf);
                return new ResponseEntity<>(txn, HttpStatus.BAD_REQUEST);
            }

            txn.setStatus("SUCCESS");
            repo.save(txn);
            return ResponseEntity.ok(txn);

        } catch (Exception e) {
            txn.setStatus("ERROR");
            repo.save(txn);
            return new ResponseEntity<>(txn, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

