package com.tejas.transactionservice.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	
	@GetMapping("/ledger/debit/{accountNumber}")
	public ResponseEntity<Page<TransactionLedgerRecord>> getDebitLedgerTransaction(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, 
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return accountService.getDebitTransaction(accountNumber, from, to, pageable);
	}
	
	@GetMapping("/ledger/credit/{accountNumber}")
	public ResponseEntity<Page<TransactionLedgerRecord>> getCreditLedgerTransaction(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, 
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return accountService.getCreditTransaction(accountNumber, from, to, pageable);
	}
	
	@GetMapping("/ledger/all/{accountNumber}")	
	public ResponseEntity<Page<TransactionLedgerRecord>> getAllLedgerTransactions(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return accountService.getAllTransaction(accountNumber, from, to, pageable);
	}	
}
