package com.tejas.bankpaymentservice.controllers;

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

import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankpaymentservice.models.InitiatePaymentDTO;
import com.tejas.bankpaymentservice.models.Payment;
import com.tejas.bankpaymentservice.models.PaymentResponse;
import com.tejas.bankpaymentservice.services.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {
	@Autowired
	PaymentService paymentService;
	
	@GetMapping
	public ResponseEntity<List<Payment>> getAllAccounts() {
		return paymentService.getAllPayments();
	}
	
	@PostMapping("/initiate")
	public ResponseEntity<PaymentResponse> createAccount(@Valid @RequestBody InitiatePaymentDTO initiateReq) {
		return paymentService.initiateRequest(initiateReq);
	}
	
	@GetMapping("/{account}")
	public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String account) {
		return accountService.getAccountByAccountNumber(account);
	}
	
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Account>> getAccountByUserId(@PathVariable int userId) {
		return accountService.getAccountsByUserId(userId);
	}
	
	@GetMapping("/accountIds")
	public ResponseEntity<List<Long>> getAccountIdsByUserId() {
		return accountService.getAccountIdsByUserId();
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
