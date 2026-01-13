package com.tejas.bankaccountservice.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejas.bankaccountservice.models.Account;
import com.tejas.bankingcommon.enums.AccountType;

public interface AccountRepo extends JpaRepository<Account, Integer>{
	public Optional<Account> getByAccountnumber(String accountNumber);

	public Optional<List<Account>> getByUserid(long id);
	
	public Optional<List<Account>> getByAccountType(AccountType type);

	public Optional<Account> getById(long accountId);
}
