package com.tejas.bankcardservice.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejas.bankcardservice.model.Card;

public interface CardRepo extends JpaRepository<Card, Long>{
	public Optional<Card> getByAccountId(long accountId);
	
	public Optional<Card> getByCardNumber(String hashedCardNumber);

	public boolean existsByCardNumber(String hashedCardNumber);

	public boolean existsByAccountId(long accountId);
}
