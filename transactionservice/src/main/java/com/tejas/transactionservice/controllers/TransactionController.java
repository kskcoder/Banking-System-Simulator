package com.tejas.transactionservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.transactionservice.models.Transaction;
import com.tejas.transactionservice.models.TransactionLedgerRecord;
import com.tejas.transactionservice.models.TransferRequest;
import com.tejas.transactionservice.services.TransactionService;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
	@Autowired
	TransactionService accountService;

	@GetMapping("/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Transaction>> getAllTransfers() {
		return accountService.getAllTransfers();
	}
	
	@GetMapping("/ledger/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<TransactionLedgerRecord>> getAllLedgerTransactions() {
		return accountService.getAllLedgerTransaction();
	}
		
	@PostMapping("/transfer")
	public ResponseEntity<Transaction> transfer(@RequestBody TransferRequest request) {
		return accountService.transfer(request);
	}
	
	@GetMapping("/transaction/{txnId}")
	public ResponseEntity<Transaction> getTransaction(@PathVariable int txnId) {
		return accountService.getTransaction(txnId);
	}
	
	@GetMapping("/debit/{accountNumber}")
	public ResponseEntity<List<TransactionLedgerRecord>> getDebitLedgerTransaction(@PathVariable String accountNumber) {
		return accountService.getDebitTransaction(accountNumber);
	}
	
	@GetMapping("/credit/{accountNumber}")
	public ResponseEntity<List<TransactionLedgerRecord>> getCreditLedgerTransaction(@PathVariable String accountNumber) {
		return accountService.getCreditTransaction(accountNumber);
	}
	
	@GetMapping("/all/{accountNumber}")
	public ResponseEntity<List<TransactionLedgerRecord>> getAllLedgerTransactions(@PathVariable String accountNumber) {
		return accountService.getAllTransaction(accountNumber);
	}	
}
