package com.tejas.transactionservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tejas.transactionservice.models.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Integer>{
	public abstract Transaction findById(long Id);
}
