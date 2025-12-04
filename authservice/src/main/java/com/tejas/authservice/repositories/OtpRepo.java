package com.tejas.authservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejas.authservice.models.Otp;

public interface OtpRepo extends JpaRepository<Otp, Long> {
	
}
