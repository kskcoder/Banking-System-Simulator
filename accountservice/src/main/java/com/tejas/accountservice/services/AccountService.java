package com.tejas.accountservice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.tejas.accountservice.models.Account;
import com.tejas.accountservice.models.CreateAccountDTO;
import com.tejas.accountservice.repositories.AccountRepo;
import com.tejas.accountservice.utils.AccountUtils;
import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankingcommon.exceptions.ForbiddenException;
import com.tejas.bankingcommon.exceptions.GeneralServerException;
import com.tejas.bankingcommon.exceptions.NoContentException;
import com.tejas.bankingcommon.exceptions.NotFoundException;

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
		
		try {
			repo.save(account);
		} catch (Exception e) {
			throw new GeneralServerException();
		}
		
		return ResponseEntity.ok().body(account);
	}

	public ResponseEntity<Account> getAccountByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (account != null && (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin())) {
			return ResponseEntity.ok().body(account);
		}
			
		throw new ForbiddenException("You do not have permission to access this resource.");
	}
	
	public ResponseEntity<List<Account>> getAccountsByUserId(int id) {
		List<Account> accounts = repo.getByUserid(id).get();
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (accounts != null) {
			if (String.valueOf(accounts.stream().findFirst().get().getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				return ResponseEntity.ok().body(accounts);
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
		throw new NotFoundException("Requested account not found.");
	}
	
	public ResponseEntity<Double> getBalanceByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (account != null) {
			if (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				return ResponseEntity.ok().body(account.getBalance());
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	public ResponseEntity<String> closeAccount(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		
		if (account != null) {
			if (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				repo.delete(account);
				return ResponseEntity.ok().body("Successful");
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
		
		throw new NotFoundException("Requested account not found.");		
	}

	public ResponseEntity<List<Account>> getAllAccounts() {
		List<Account> accounts = repo.findAll();
		
		if (!accounts.isEmpty()) {
			return ResponseEntity.ok().body(accounts);
		}
		
		throw new NoContentException("No accounts not found.");
	}

	public ResponseEntity<Boolean> isOwnerOfAccountNumber(String accountNumber) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		Account account = repo.getByAccountnumber(accountNumber).get();
		if (account != null) {
			boolean isOwner = String.valueOf(account.getUserid()).equals(userId);
			return ResponseEntity.ok().body(isOwner);
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	public ResponseEntity<Boolean> isOwnerOfAccountId(long accountId) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		Account account = repo.getById(accountId).get();
		if (account != null) {
			boolean isOwner = String.valueOf(account.getUserid()).equals(userId);
			return ResponseEntity.ok().body(isOwner);
		}
			
		throw new NotFoundException("Requested account not found.");
	}

	public ResponseEntity<AccountType> getAccountTypeByAccountId(long accountId) {
		Account account = repo.getById(accountId).get();
		if (account != null) {
			return ResponseEntity.ok().body(account.getAccountType());
		}
			
		throw new NotFoundException("Requested account not found.");
	}
}
