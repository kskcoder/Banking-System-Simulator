package com.tejas.bankaccountservice.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.tejas.bankaccountservice.feign.AuthInterface;
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

import feign.FeignException;

@Service
public class AccountService {
	@Autowired
	private AccountRepo repo;
	
	@Autowired
	private CacheManager cacheManager;
	
	@Autowired
	private AuthInterface authInterface;
	
	public void evictAccountCache(Long accountId) {
		Cache accountDetailsCache = cacheManager.getCache("account_details");
		if (accountDetailsCache != null && accountId != null) {
			accountDetailsCache.evict(accountId);
		}
		
		Cache balanceCache = cacheManager.getCache("balance");
		if (balanceCache != null && accountId != null) {
			balanceCache.evict(accountId);
		}
		
		Cache accountTypeCache = cacheManager.getCache("account_type");
		if (accountTypeCache != null && accountId != null) {
			accountTypeCache.evict(accountId);
		}
		
		Cache userIdCache = cacheManager.getCache("userId");
		if (userIdCache != null && accountId != null) {
			userIdCache.evict(accountId);
		}
	}
	
	private void evictUserAccountsCache(Long userId) {
		Cache userAccountsCache = cacheManager.getCache("all_accounts_of_userid");
		if (userAccountsCache != null && userId != null) {
			userAccountsCache.evict(userId);
		}
	}
	
	private void evictAllAccountIdsCache() {
		Cache allAccountIdsCache = cacheManager.getCache("all_account_ids");
		if (allAccountIdsCache != null) {
			allAccountIdsCache.clear();
		}
	}
	
    public String generateAccountNumber(int userId) {
        return "AC" + userId + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
	
	public Account createAccount(CreateAccountDTO accountReq) {
		boolean userExistsResponse = false;
		try {
			userExistsResponse = authInterface.userExists(Long.valueOf(accountReq.getUserId())).getBody();
		} catch (FeignException e) {
			throw new GeneralServerException();
		}
		
		if (!userExistsResponse) {
			throw new GeneralServerException();
		}
		
		String userId = AccountUtils.getUserId();
		
		if (!(String.valueOf(accountReq.getUserId()).equals(userId) || AccountUtils.isAdmin())) {
			throw new ForbiddenException("You do not have permission to access this resource.");
		}
		
		Account account = new Account();
		account.setUserid(accountReq.getUserId());
		account.setAccountnumber(generateAccountNumber(accountReq.getUserId()));
		account.setAccountType(accountReq.getAccountType());
		account.setBalance(accountReq.getInitialAmount());
		
		try {
			Account savedAccount = repo.save(account);
			evictUserAccountsCache(Long.valueOf(savedAccount.getUserid()));
			evictAllAccountIdsCache();
			return savedAccount;
		} catch (Exception e) {
			throw new GeneralServerException();
		}
	}

	public Account getAccountByAccountNumber(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).orElseThrow(() -> new NotFoundException("Requested account not found."));
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
		List<Account> accounts = repo.getByUserid(userId).orElseThrow(() -> new NotFoundException("Requested account not found."));
		
		if (!accounts.isEmpty()) {
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
		Account account = repo.getById(accountId).orElseThrow(() -> new NotFoundException("Requested account not found."));
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
	    return Double.valueOf(account.getBalance());
	}
	
	public String closeAccount(String accountNumber) {
		Account account = repo.getByAccountnumber(accountNumber).orElseThrow(() -> new NotFoundException("Requested account not found."));
		String userId = AccountUtils.getUserId();
		
		if (account != null) {
			if (String.valueOf(account.getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				Long accountId = account.getId();
				Long accountUserId = Long.valueOf(account.getUserid());
				repo.delete(account);
				evictAccountCache(accountId);
				evictUserAccountsCache(accountUserId);
				evictAllAccountIdsCache();
				return "Account Closed Successfully!";
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
		Account account = repo.getByAccountnumber(accountNumber).orElseThrow(() -> new NotFoundException("Requested account not found."));
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
		Account account = repo.getById(accountId).orElseThrow(() -> new NotFoundException("Requested account not found."));
		if (account != null) {
			boolean isOwner = String.valueOf(account.getUserid()).equals(userId);
			return isOwner;
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	@Cacheable(value = "account_type", key = "#accountId", unless="#result == null")
	public AccountType getAccountTypeByAccountId(long accountId) {
		Account account = repo.getById(accountId).orElseThrow(() -> new NotFoundException("Requested account not found."));
		if (account != null) {
			return account.getAccountType();
		}
			
		throw new NotFoundException("Requested account not found.");
	}

	public List<Long> getAccountIdsByUserId() {
		String userId = AccountUtils.getUserId();
		List<Account> accounts = repo.getByUserid(Long.parseLong(userId)).orElseThrow(() -> new NoContentException("No accounts found."));
		
		if (!accounts.isEmpty()) {
			if (String.valueOf(accounts.stream().findFirst().get().getUserid()).equals(userId) || AccountUtils.isAdmin()) {
				List<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());
				return accountIds;
			} else {
				throw new ForbiddenException("You do not have permission to access this resource.");
			}
		}
		
		throw new NoContentException("No accounts found.");
	}
	
	@Cacheable(value = "all_accounts_of_userid", key = "T(com.tejas.bankaccountservice.utils.AccountUtils).getUserId()", unless="#result == null || #result.isEmpty()")
	public List<Account> getMyAccounts() {
		String userId = AccountUtils.getUserId();
		List<Account> accounts = repo.getByUserid(Long.parseLong(userId)).orElseThrow(() -> new NoContentException("No accounts found."));
		
		if (!accounts.isEmpty()) {
			return accounts;
		}
		
		throw new NoContentException("No accounts found.");
	}
	
	@Cacheable(value = "userId", key = "#accountId", unless="#result == null")
	public Long getuserIdByAccountId(long accountId) {
		Account account = repo.getById(accountId).orElseThrow(() -> new NotFoundException("Requested account not found."));
		if (account != null) {
			return Long.valueOf(account.getUserid());
		}
			
		throw new NotFoundException("Requested account not found.");
	}
	
	@Cacheable(value = "account_number", key = "#accountId", unless="#result == null")
	public String getAccountNumberByAccountId(long accountId) {
		Account account = repo.getById(accountId).orElseThrow(() -> new NotFoundException("Requested account not found."));
		if (account != null) {
			return account.getAccountnumber();
		}
		throw new NotFoundException("Requested account not found.");
	}
	
	public Boolean accountExists(Long accountId) {
		String requestingUserId = AccountUtils.getUserId();
		Account account = repo.getById(accountId).orElseThrow(() -> new NotFoundException("Requested account not found."));
		
		long userId = account.getUserid();
		
		if (AccountUtils.isAdmin() || String.valueOf(userId).equals(requestingUserId)) {
			return repo.getById(accountId).isPresent();
		} else {
			throw new ForbiddenException("You do not have permission to access this resource.");
		}
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
