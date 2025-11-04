package com.tejas.accountservice.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
	public ResponseEntity<Account> createAccount(@RequestBody CreateAccountDTO accountReq) {
		return accountService.createAccount(accountReq);
	}
	
	@GetMapping("/{account}")
	public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String account) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		return accountService.getAccountByAccountNumber(userId, account);
	}
	
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Account>> getAccountByUserId(@PathVariable int userId) {
		return accountService.getAccountByUserId(userId);
	}
	
	@DeleteMapping("/close/{account}")
	public ResponseEntity<String> closeAccount(@PathVariable String account) {
		return accountService.closeAccount(account);
	}
}
