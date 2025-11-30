package com.tejas.accountservice.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
		String userId = AccountUtils.getUserId();
		
		if (account != null && (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin())) {
			return ResponseEntity.ok().body(account);
		}
			
		throw new ForbiddenException("You do not have permission to access this resource.");
	}
	
	public ResponseEntity<List<Account>> getAccountsByUserId(int id) {
		List<Account> accounts = repo.getByUserid(id).get();
		String userId = AccountUtils.getUserId();
		
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
		String userId = AccountUtils.getUserId();
		
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
		String userId = AccountUtils.getUserId();
		
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

	public ResponseEntity<Boolean> isOwnerOfAccountNumber(String accountNumber) {
		String userId = AccountUtils.getUserId();
		Account account = repo.getByAccountnumber(accountNumber).get();
		if (account != null) {
			boolean isOwner = String.valueOf(account.getUserid()).equals(userId);
			return ResponseEntity.ok().body(isOwner);
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	public ResponseEntity<Boolean> isOwnerOfAccountId(long accountId) {
		String userId = AccountUtils.getUserId();
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

	public ResponseEntity<List<Long>> getAccountIdsByUserId() {
		String userId = AccountUtils.getUserId();
		List<Account> accounts = repo.getByUserid(Long.parseLong(userId)).get();
		
		if (accounts != null) {
			if (String.valueOf(accounts.stream().findFirst().get().getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				List<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());
				return ResponseEntity.ok().body(accountIds);
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
		
		throw new NoContentException("No accounts found.");
	}
	
	public ResponseEntity<Long> getuserIdByAccountId(long accountId) {
		Account account = repo.getById(accountId).get();
		if (account != null) {
			return ResponseEntity.ok().body(account.getUserid());
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	//Admin related functions
	public ResponseEntity<List<Account>> getAllAccounts() {
		if (!AccountUtils.isAdmin()) {throw new ForbiddenException("You do not have permission to access this resource.");}
		List<Account> accounts = repo.findAll();
		
		if (!accounts.isEmpty()) {
			return ResponseEntity.ok().body(accounts);
		}
		
		throw new NoContentException("No accounts found.");
	}
}
