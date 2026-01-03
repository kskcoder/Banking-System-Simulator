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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class AccountController {
	@Autowired
	AccountService accountService;
	
	@GetMapping
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<Account>> getAllAccounts() {
		return ResponseEntity.ok().body(accountService.getAllAccounts());
	}
	
	@PostMapping("/create")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountDTO accountReq) {
		return ResponseEntity.ok().body(accountService.createAccount(accountReq));
	}
	
	@GetMapping("/{account}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String account) {
		return ResponseEntity.ok().body(accountService.getAccountByAccountNumber(account));
	}
	
	@GetMapping("/user/{userId}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<List<Account>> getAccountByUserId(@PathVariable int userId) {
		return ResponseEntity.ok().body(accountService.getAccountsByUserId(userId));
	}
	
	@DeleteMapping("/close/{account}")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<String> closeAccount(@PathVariable String account) {
		return ResponseEntity.ok().body(accountService.closeAccount(account));
	}
	
	//INTERNAL METHODS
	@Hidden
	@GetMapping("/accountnumber/{accountNumber}/is-owner")
	public ResponseEntity<Boolean> isOwnerOfAccountNumber(@PathVariable String accountNumber) {
		return ResponseEntity.ok().body(accountService.isOwnerOfAccountNumber(accountNumber));
	}
	
	@Hidden
	@GetMapping("/accountid/{accountId}/is-owner")
	public ResponseEntity<Boolean> isOwnerOfAccount(@PathVariable long accountId) {
		return ResponseEntity.ok().body(accountService.isOwnerOfAccountId(accountId));
	}
	
	@Hidden
	@GetMapping("/accountIds")
	public ResponseEntity<List<Long>> getAccountIdsByUserId() {
		return ResponseEntity.ok().body(accountService.getAccountIdsByUserId());
	}
	
	@Hidden
	@GetMapping("/accountid/{accountId}/type")
	public ResponseEntity<AccountType> getAccountTypeByAccountId(@PathVariable long accountId) {
		return ResponseEntity.ok().body(accountService.getAccountTypeByAccountId(accountId));
	}
	
	@Hidden
	@GetMapping("/accountid/{accountId}/userid")
	public ResponseEntity<Long> getuserIdByAccountId(@PathVariable long accountId) {
		return ResponseEntity.ok().body(accountService.getuserIdByAccountId(accountId));
	}
}
