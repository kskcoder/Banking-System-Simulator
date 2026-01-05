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

import com.tejas.bankingcommon.dto.DepositRequest;
import com.tejas.bankingcommon.dto.TransferRequest;
import com.tejas.bankingcommon.dto.WithdrawRequest;
import com.tejas.banktransactionservice.models.Transaction;
import com.tejas.banktransactionservice.models.TransactionLedgerRecord;
import com.tejas.banktransactionservice.services.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
	@Autowired
	TransactionService trService;

	@GetMapping("/all")
	@Operation(summary = "Get all transactions", description = "Retrieve all transactions (Admin only)")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<Transaction>> getAllTransfers() {
		return ResponseEntity.ok().body(trService.getAllTransfers());
	}
	
	@GetMapping("/ledger/all")
	@Operation(summary = "Get all ledger transactions", description = "Retrieve all ledger transactions (Admin only)")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<TransactionLedgerRecord>> getAllLedgerTransactions() {
		return ResponseEntity.ok().body(trService.getAllLedgerTransaction());
	}
		
	@PostMapping("/transfer")
	@Operation(summary = "Transfer funds", description = "Transfer funds between accounts")
	@SecurityRequirement(name = "bearerAuth")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "Transfer request details",
		required = true,
		content = @Content(schema = @Schema(implementation = TransferRequest.class))
	)
	public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
		return ResponseEntity.ok().body(trService.transfer(request));
	}
	
	@PostMapping("/deposit")
	@Operation(summary = "Cash deposit", description = "Deposit cash into an account (Admin only)")
	@SecurityRequirement(name = "bearerAuth")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "Deposit request details",
		required = true,
		content = @Content(schema = @Schema(implementation = DepositRequest.class))
	)
	public ResponseEntity<Transaction> deposit(@Valid @RequestBody DepositRequest request) {
		return ResponseEntity.ok().body(trService.deposit(request));
	}
	
	@PostMapping("/withdraw")
	@Operation(summary = "Cash withdrawal", description = "Withdraw cash from an account (Admin only)")
	@SecurityRequirement(name = "bearerAuth")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "Withdrawal request details",
		required = true,
		content = @Content(schema = @Schema(implementation = WithdrawRequest.class))
	)
	public ResponseEntity<Transaction> withdraw(@Valid @RequestBody WithdrawRequest request) {
		return ResponseEntity.ok().body(trService.withdraw(request));
	}
	
	@GetMapping("/transaction/{txnId}")
	@Operation(summary = "Get transaction by ID", description = "Retrieve transaction details by transaction ID")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "txnId",
		in = ParameterIn.PATH,
		required = true,
		description = "Transaction ID",
		example = "1",
		schema = @Schema(type = "integer", format = "int32")
	)
	public ResponseEntity<Transaction> getTransaction(@PathVariable int txnId) {
		return ResponseEntity.ok().body(trService.getTransaction(txnId));
	}
	
	@GetMapping("/ledger/debit/{accountNumber}")
	@Operation(summary = "Get debit transactions", description = "Retrieve paginated debit transactions for an account")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountNumber",
		in = ParameterIn.PATH,
		required = true,
		description = "Account number",
		example = "AC123456789",
		schema = @Schema(type = "string")
	)
	@Parameter(
		name = "from",
		in = ParameterIn.QUERY,
		required = false,
		description = "Start date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
	@Parameter(
		name = "to",
		in = ParameterIn.QUERY,
		required = false,
		description = "End date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
	public ResponseEntity<Page<TransactionLedgerRecord>> getDebitLedgerTransaction(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, 
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok().body(trService.getDebitTransaction(accountNumber, from, to, pageable));
	}
	
	@GetMapping("/ledger/credit/{accountNumber}")
	@Operation(summary = "Get credit transactions", description = "Retrieve paginated credit transactions for an account")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountNumber",
		in = ParameterIn.PATH,
		required = true,
		description = "Account number",
		example = "AC123456789",
		schema = @Schema(type = "string")
	)
	@Parameter(
		name = "from",
		in = ParameterIn.QUERY,
		required = false,
		description = "Start date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
	@Parameter(
		name = "to",
		in = ParameterIn.QUERY,
		required = false,
		description = "End date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
	public ResponseEntity<Page<TransactionLedgerRecord>> getCreditLedgerTransaction(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to, 
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok().body(trService.getCreditTransaction(accountNumber, from, to, pageable));
	}
	
	@GetMapping("/ledger/all/{accountNumber}")
	@Operation(summary = "Get all ledger transactions", description = "Retrieve paginated all transactions (debit and credit) for an account")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountNumber",
		in = ParameterIn.PATH,
		required = true,
		description = "Account number",
		example = "AC123456789",
		schema = @Schema(type = "string")
	)
	@Parameter(
		name = "from",
		in = ParameterIn.QUERY,
		required = false,
		description = "Start date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
	@Parameter(
		name = "to",
		in = ParameterIn.QUERY,
		required = false,
		description = "End date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
	public ResponseEntity<Page<TransactionLedgerRecord>> getAllLedgerTransactions(
			@PathVariable String accountNumber,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@PageableDefault(size=10, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok().body(trService.getAllTransaction(accountNumber, from, to, pageable));
	}
	
	@GetMapping("/ledger/statement/{accountNumber}")
	@Operation(summary = "Get account statement", description = "Download account statement as PDF for an account")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "accountNumber",
		in = ParameterIn.PATH,
		required = true,
		description = "Account number",
		example = "AC123456789",
		schema = @Schema(type = "string")
	)
	@Parameter(
		name = "from",
		in = ParameterIn.QUERY,
		required = false,
		description = "Start date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
	@Parameter(
		name = "to",
		in = ParameterIn.QUERY,
		required = false,
		description = "End date/time (ISO format)",
		schema = @Schema(type = "string", format = "date-time")
	)
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
