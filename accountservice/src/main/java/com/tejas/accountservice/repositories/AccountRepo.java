package com.tejas.accountservice.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejas.accountservice.models.Account;

public interface AccountRepo extends JpaRepository<Account, Integer>{
	public Optional<Account> getByAccountnumber(String accountNumber);

	public Optional<List<Account>> getByUserid(long id);
}
