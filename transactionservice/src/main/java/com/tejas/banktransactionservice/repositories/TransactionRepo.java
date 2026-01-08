package com.tejas.banktransactionservice.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tejas.banktransactionservice.models.Transaction;

@Repository
public interface TransactionRepo extends JpaRepository<Transaction, Long>{
	public abstract Optional<Transaction> findById(long Id);
}
