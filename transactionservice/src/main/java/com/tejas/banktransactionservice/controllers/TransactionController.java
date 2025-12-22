package com.tejas.banktransactionservice.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankingcommon.dto.TransferRequest;
import com.tejas.banktransactionservice.models.Transaction;
import com.tejas.banktransactionservice.models.TransactionLedgerRecord;
import com.tejas.banktransactionservice.services.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
	@Autowired
	TransactionService trService;

	@GetMapping("/all")
	public ResponseEntity<List<Transaction>> getAllTransfers() {
		return ResponseEntity.ok().body(trService.getAllTransfers());
	}
	
	@GetMapping("/ledger/all")
	public ResponseEntity<List<TransactionLedgerRecord>> getAllLedgerTransactions() {
		return ResponseEntity.ok().body(trService.getAllLedgerTransaction());
	}
		
	@PostMapping("/transfer")
	public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
		return ResponseEntity.ok().body(trService.transfer(request));
	}
	
	@GetMapping("/transaction/{txnId}")
	public ResponseEntity<Transaction> getTransaction(@PathVariable int txnId) {
		return ResponseEntity.ok().body(trService.getTransaction(txnId));
	}
	
	@GetMapping("/ledger/debit/{accountNumber}")
	public ResponseEntity<Page<TransactionLedgerRecord>> getDebitLedgerTransaction(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, 
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok().body(trService.getDebitTransaction(accountNumber, from, to, pageable));
	}
	
	@GetMapping("/ledger/credit/{accountNumber}")
	public ResponseEntity<Page<TransactionLedgerRecord>> getCreditLedgerTransaction(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, 
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok().body(trService.getCreditTransaction(accountNumber, from, to, pageable));
	}
	
	@GetMapping("/ledger/all/{accountNumber}")	
	public ResponseEntity<Page<TransactionLedgerRecord>> getAllLedgerTransactions(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok().body(trService.getAllTransaction(accountNumber, from, to, pageable));
	}
	
	@GetMapping("/ledger/statement/{accountNumber}")	
	public ResponseEntity<byte[]> getStatement(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
		byte[] data = trService.getStatement(accountNumber, from, to);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		
		DateTimeFormatter ftr = DateTimeFormatter.ofPattern("ddMMMyyyyHHmm");
		String currentTime = LocalDateTime.now().format(ftr);
		
		headers.set(HttpHeaders.CONTENT_DISPOSITION,
				"attachment, filename=statement_"+accountNumber+currentTime);
		
		return ResponseEntity.ok().headers(headers).body(data);
	}
}
