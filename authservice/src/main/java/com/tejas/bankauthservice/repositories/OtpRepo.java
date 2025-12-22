package com.tejas.bankauthservice.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tejas.bankauthservice.models.Otp;
import com.tejas.bankingcommon.dto.MessageType;

public interface OtpRepo extends JpaRepository<Otp, Long> {
	public abstract Optional<Otp> findByReferenceIdAndType(String referenceId, MessageType type);
}
