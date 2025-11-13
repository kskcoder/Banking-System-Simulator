package com.tejas.transactionservice.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tejas.transactionservice.models.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Integer>{
	public abstract Transaction findById(long Id);
	
	public abstract List<Transaction> findByFromAccount(String fromAccountNum);

	public abstract List<Transaction> findByToAccount(String accountNum);
}
