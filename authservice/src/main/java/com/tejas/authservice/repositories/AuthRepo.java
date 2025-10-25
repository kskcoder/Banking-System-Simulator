package com.tejas.authservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tejas.authservice.models.User;

@Repository
public interface AuthRepo extends JpaRepository<User, Integer>{
	public User getByUsername(String username);
}
