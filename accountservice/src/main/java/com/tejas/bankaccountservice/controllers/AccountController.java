package com.tejas.bankaccountservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.bankaccountservice.models.Account;
import com.tejas.bankaccountservice.models.CreateAccountDTO;
import com.tejas.bankaccountservice.services.AccountService;
import com.tejas.bankingcommon.enums.AccountType;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class AccountController {
	@Autowired
	AccountService accountService;
	
	@GetMapping
	@Operation(summary = "Get all accounts", description = "Retrieve all accounts (Admin only)")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<Account>> getAllAccounts() {
		return ResponseEntity.ok().body(accountService.getAllAccounts());
	}
	
	@PostMapping("/create")
	@Operation(summary = "Create account", description = "Create a new bank account")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountDTO accountReq) {
		return ResponseEntity.ok().body(accountService.createAccount(accountReq));
	}
	
	@GetMapping("/{account}")
	@Operation(summary = "Get account by account number", description = "Retrieve account details by account number")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "account",
		in = ParameterIn.PATH,
		required = true,
		description = "Account number",
		example = "AC123456789",
		schema = @Schema(type = "string")
	)
	public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String account) {
		return ResponseEntity.ok().body(accountService.getAccountByAccountNumber(account));
	}
	
	@GetMapping("/my-accounts")
	@Operation(summary = "Get my accounts", description = "Retrieve all accounts for the authenticated user")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<Account>> getMyAccounts() {
		return ResponseEntity.ok().body(accountService.getMyAccounts());
	}
	
	@GetMapping("/user/{userId}")
	@Operation(summary = "Get accounts by user ID", description = "Retrieve all accounts for a specific user")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "userId",
		in = ParameterIn.PATH,
		required = true,
		description = "User ID",
		example = "1",
		schema = @Schema(type = "integer", format = "int32")
	)
	public ResponseEntity<List<Account>> getAccountByUserId(@PathVariable int userId) {
		return ResponseEntity.ok().body(accountService.getAccountsByUserId(userId));
	}
	
	@DeleteMapping("/close/{account}")
	@Operation(summary = "Close account", description = "Close an existing account by account number")
	@SecurityRequirement(name = "bearerAuth")
	@Parameter(
		name = "account",
		in = ParameterIn.PATH,
		required = true,
		description = "Account number to close",
		example = "AC123456789",
		schema = @Schema(type = "string")
	)
	public ResponseEntity<String> closeAccount(@PathVariable String account) {
		return ResponseEntity.ok().body(accountService.closeAccount(account));
	}
	
	//INTERNAL METHODS
	@Hidden
	@GetMapping("/accountnumber/{accountNumber}/is-owner")
	@Parameter(
		name = "accountNumber",
		in = ParameterIn.PATH,
		required = true,
		description = "Account number to check ownership",
		example = "AC123456789",
		schema = @Schema(type = "string")
	)
	public ResponseEntity<Boolean> isOwnerOfAccountNumber(@PathVariable String accountNumber) {
		return ResponseEntity.ok().body(accountService.isOwnerOfAccountNumber(accountNumber));
	}
	
	@Hidden
	@GetMapping("/accountid/{accountId}/is-owner")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to check ownership",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<Boolean> isOwnerOfAccountId(@PathVariable long accountId) {
		return ResponseEntity.ok().body(accountService.isOwnerOfAccountId(accountId));
	}
	
	@Hidden
	@GetMapping("/accountIds")
	public ResponseEntity<List<Long>> getAccountIdsByUserId() {
		return ResponseEntity.ok().body(accountService.getAccountIdsByUserId());
	}
	
	@Hidden
	@GetMapping("/accountid/{accountId}/type")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to get account type",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<AccountType> getAccountTypeByAccountId(@PathVariable long accountId) {
		return ResponseEntity.ok().body(accountService.getAccountTypeByAccountId(accountId));
	}
	
	@Hidden
	@GetMapping("/accountid/{accountId}/userid")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to get user ID",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<Long> getuserIdByAccountId(@PathVariable long accountId) {
		return ResponseEntity.ok().body(accountService.getuserIdByAccountId(accountId));
	}
	
	@Hidden
	@GetMapping("/exists/{accountId}")
	@Parameter(
		name = "accountId",
		in = ParameterIn.PATH,
		required = true,
		description = "Account ID to check existence",
		example = "1",
		schema = @Schema(type = "integer", format = "int64")
	)
	public ResponseEntity<Boolean> accountExists(@Valid @PathVariable Long accountId) {
		return ResponseEntity.ok().body(accountService.accountExists(accountId));
	}
}
