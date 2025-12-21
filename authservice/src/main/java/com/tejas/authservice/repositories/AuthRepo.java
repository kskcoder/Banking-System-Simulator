package com.tejas.authservice.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tejas.authservice.models.User;

@Repository
public interface AuthRepo extends JpaRepository<User, Long>{
	public Optional<User> getByUsername(String username);

	public Optional<User> getByEmail(String email);

	public Optional<User> getById(long userId);
}
