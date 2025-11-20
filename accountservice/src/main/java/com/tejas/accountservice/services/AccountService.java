package com.tejas.accountservice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tejas.accountservice.models.Account;
import com.tejas.accountservice.models.CreateAccountDTO;
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
		account.setAccountType(accountReq.getAccountType());
		account.setBalance(accountReq.getInitialAmount());
		
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
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (accounts != null) {
			if (String.valueOf(accounts.stream().findFirst().get().getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				return new ResponseEntity<>(accounts, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
			}
		}
			
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<Double> getBalanceByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (account != null) {
			if (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				return new ResponseEntity<>(account.getBalance(), HttpStatus.OK);
			} else {
				return new ResponseEntity<>(0.0, HttpStatus.UNAUTHORIZED);
			}
		}
			
		return new ResponseEntity<>(0.0, HttpStatus.NOT_FOUND);
	}
	
	public ResponseEntity<String> closeAccount(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (account != null) {
			if (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				repo.delete(account);
				return new ResponseEntity<>("Successful", HttpStatus.OK);
			} else {
				return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
			}
		}
		
		return new ResponseEntity<>("Could not find account!", HttpStatus.NOT_FOUND);		
	}

	public ResponseEntity<List<Account>> getAllAccounts() {
		List<Account> accounts = repo.findAll();
		
		if (!accounts.isEmpty()) {
			return new ResponseEntity<>(accounts, HttpStatus.OK);
		}
		return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
	}

	public ResponseEntity<String> getUserIdByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		
		if (account != null) {
			return new ResponseEntity<>(String.valueOf(account.getUserid()), HttpStatus.OK);
		}
			
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}

	public ResponseEntity<Boolean> isOwnerOfAccount(String accountNumber) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		Account account = repo.getByAccountnumber(accountNumber).get();
		if (account != null) {
			return new ResponseEntity<>(String.valueOf(account.getUserid()).equals(userId), HttpStatus.OK);
		}
			
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
}
