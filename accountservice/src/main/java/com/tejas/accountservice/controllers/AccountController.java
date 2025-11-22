package com.tejas.accountservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tejas.accountservice.models.Account;
import com.tejas.accountservice.models.CreateAccountDTO;
import com.tejas.accountservice.services.AccountService;
import com.tejas.bankingcommon.enums.AccountType;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class AccountController {
	@Autowired
	AccountService accountService;
	
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<Account>> getAllAccounts() {
		return accountService.getAllAccounts();
	}
	
	@PostMapping("/create")
	public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountDTO accountReq) {
		return accountService.createAccount(accountReq);
	}
	
	@GetMapping("/{account}")
	public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String account) {
		return accountService.getAccountByAccountNumber(account);
	}
	
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Account>> getAccountByUserId(@PathVariable int userId) {
		return accountService.getAccountsByUserId(userId);
	}
	
	@DeleteMapping("/close/{account}")
	public ResponseEntity<String> closeAccount(@PathVariable String account) {
		return accountService.closeAccount(account);
	}
	
	@GetMapping("/accountnumber/{accountNumber}/is-owner")
	public ResponseEntity<Boolean> isOwnerOfAccount(@PathVariable String accountNumber) {
		return accountService.isOwnerOfAccountNumber(accountNumber);
	}
	
	@GetMapping("/accountid/{accountId}/is-owner")
	public ResponseEntity<Boolean> isOwnerOfAccount(@PathVariable long accountId) {
		return accountService.isOwnerOfAccountId(accountId);
	}
	
	@GetMapping("/accountid/{accountId}/type")
	public ResponseEntity<AccountType> getAccountTypeByAccountId(@PathVariable long accountId) {
		return accountService.getAccountTypeByAccountId(accountId);
	}
}
