package com.tejas.bankaccountservice.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.tejas.bankaccountservice.models.Account;
import com.tejas.bankaccountservice.models.CreateAccountDTO;
import com.tejas.bankaccountservice.repositories.AccountRepo;
import com.tejas.bankaccountservice.utils.AccountUtils;
import com.tejas.bankaccountservice.utils.AuthUtils;
import com.tejas.bankingcommon.enums.AccountType;
import com.tejas.bankingcommon.enums.UserType;
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
	
	public Account createAccount(CreateAccountDTO accountReq) {
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
		
		return account;
	}

	public Account getAccountByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = AccountUtils.getUserId();
		
		if (account != null && (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin())) {
			return account;
		}
			
		throw new ForbiddenException("You do not have permission to access this resource.");
	}
	
	@CachePut(value="account_details", key="#account.id", unless="#result == null")
	protected Account getCachedAccount(Account account) {
		return account;
	}
	
	public List<Account> getAccountsByUserId(int userId) {
		List<Account> accounts = repo.getByUserid(userId).get();
		
		if (accounts != null) {
			if (isOwnerOfAccountId(accounts.stream().findFirst().get().getId())) {
				return accounts;
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
		throw new NotFoundException("Requested account not found.");
	}
	
	@CachePut(value="all_accounts_of_userid", key="#userId", unless="#result == null || #result.isEmpty()")
	protected List<Account> getAccountsByUserId(List<Account> accounts, Long userId) {
		return accounts;
	}
	
	public Double getBalanceByAccountId(Long accountId) {
		Account account = repo.getById(accountId).get();
		String userId = AccountUtils.getUserId();
		
		if (account != null) {
			if (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				return getCachedBalance(account);
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	@CachePut(value="balance", key="#account.id")
	protected Double getCachedBalance(Account account) {
	    return account.getBalance();
	}
	
	public String closeAccount(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).get();
		String userId = AccountUtils.getUserId();
		
		if (account != null) {
			if (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				repo.delete(account);
				return "Successful";
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
		
		throw new NotFoundException("Requested account not found.");		
	}

	public Boolean isOwnerOfAccountNumber(String accountNumber) {
		String role = AuthUtils.getRole();
		
		if (role.equals(UserType.INTERNAL_SERVICE.toString()) || role.equals(UserType.ADMIN.toString())) {
			return true;
		}
		
		String userId = AccountUtils.getUserId();
		Account account = repo.getByAccountnumber(accountNumber).get();
		if (account != null) {
			boolean isOwner = String.valueOf(account.getUserid()).equals(userId);
			return isOwner;
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	public Boolean isOwnerOfAccountId(long accountId) {
		String role = AuthUtils.getRole();
		
		if (role.equals(UserType.INTERNAL_SERVICE.toString()) || role.equals(UserType.ADMIN.toString())) {
			return true;
		}
		
		String userId = AccountUtils.getUserId();
		Account account = repo.getById(accountId).get();
		if (account != null) {
			boolean isOwner = String.valueOf(account.getUserid()).equals(userId);
			return isOwner;
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	@Cacheable(value = "account_type", key = "#accountId", unless="#result == null")
	public AccountType getAccountTypeByAccountId(long accountId) {
		Account account = repo.getById(accountId).get();
		if (account != null) {
			return account.getAccountType();
		}
			
		throw new NotFoundException("Requested account not found.");
	}

	public List<Long> getAccountIdsByUserId() {
		String userId = AccountUtils.getUserId();
		List<Account> accounts = repo.getByUserid(Long.parseLong(userId)).get();
		
		if (accounts != null) {
			if (String.valueOf(accounts.stream().findFirst().get().getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				List<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());
				return accountIds;
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
		
		throw new NoContentException("No accounts found.");
	}
	
	@Cacheable(value = "userId", key = "#accountId", unless="#result == null")
	public Long getuserIdByAccountId(long accountId) {
		Account account = repo.getById(accountId).get();
		if (account != null) {
			return account.getUserid();
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	//Admin related functions
	public List<Account> getAllAccounts() {
	    if (!AccountUtils.isAdmin()) {
	    	throw new ForbiddenException("You do not have permission to access this resource.");
	    }

	    return getAllAccountIds()
	           .stream()
	           .map(repo::getById)
	           .flatMap(Optional::stream)
	           .toList();
	}

	@Cacheable(value = "all_account_ids")
	protected List<Long> getAllAccountIds() {
	    return repo.findAll().stream()
	    		.map(Account::getId)
	    		.collect(Collectors.toList());
	}
}
