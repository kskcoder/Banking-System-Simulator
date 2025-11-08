package com.tejas.accountservice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tejas.accountservice.models.Account;
import com.tejas.accountservice.models.CreateAccountDTO;
import com.tejas.accountservice.models.TransferRequest;
import com.tejas.accountservice.repositories.AccountRepo;
import com.tejas.accountservice.utils.AccountUtils;

@Service
public class AccountService {
	@Autowired
	private AccountRepo repo;
	
    public String generateAccountNumber(int userId) {
        return "AC" + userId + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
	
	public ResponseEntity<Account> createAccount(CreateAccountDTO accountReq) {
		Account account = new Account();
		account.setUserid(accountReq.getUserId());
		account.setAccountnumber(generateAccountNumber(accountReq.getUserId()));
		account.setAccounttype(accountReq.getAccounttype());
		account.setBalance(0.0);
		
		repo.save(account);
		
		return new ResponseEntity<>(account, HttpStatus.OK);
	}

	public ResponseEntity<Account> getAccountByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (account != null && (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin())) {
			return new ResponseEntity<>(account, HttpStatus.OK);
		}
			
		return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
	}
	
	public ResponseEntity<List<Account>> getAccountByUserId(int id) {
		List<Account> accounts = repo.getByUserid(id).get();
		
		if (accounts != null) {
			return new ResponseEntity<>(accounts, HttpStatus.OK);
		}
			
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<Double> getBalanceByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		
		if (account != null) {
			return new ResponseEntity<>(account.getBalance(), HttpStatus.OK);
		}
			
		return new ResponseEntity<>(0.0, HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<String> closeAccount(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		
		if (account != null) {
			repo.delete(account);
			return new ResponseEntity<>("Successful", HttpStatus.OK);
		}
			
		return new ResponseEntity<>("Could not delete!", HttpStatus.NOT_FOUND);
		
	}

	public ResponseEntity<List<Account>> getAllAccounts() {
		List<Account> accounts = repo.findAll();
		
		if (!accounts.isEmpty()) {
			return new ResponseEntity<>(accounts, HttpStatus.OK);
		}
		return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
	}

	public ResponseEntity<String> debitAccount(TransferRequest request) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		String senderAccNumber = request.getFromAccount();
		Double amount = request.getAmount();
		
		Account account = repo.getByAccountnumber(senderAccNumber)
				.orElseThrow(() -> new RuntimeException("Account not found"));
		
		if (String.valueOf(account.getUserid()).equals(userId)) {
			if (amount > account.getBalance()) {
				return new ResponseEntity<>("Insufficient Balance", HttpStatus.BAD_REQUEST);
			}
		} else {
			return new ResponseEntity<>("Unauthorised User", HttpStatus.UNAUTHORIZED);
		}
		
		account.setBalance(account.getBalance() - amount);
		repo.save(account);
		return new ResponseEntity<>("Debited Successfully", HttpStatus.OK);
	}
	
	public ResponseEntity<String> creditAccount(TransferRequest request) {
		String senderAccNumber = request.getToAccount();
		Double amount = request.getAmount();
		
		Account account = repo.getByAccountnumber(senderAccNumber)
				.orElseThrow(() -> new RuntimeException("Account not found")); 
		
		account.setBalance(account.getBalance() + amount);
		repo.save(account);
		return new ResponseEntity<>("Credited Successfully", HttpStatus.OK);
	}
}
