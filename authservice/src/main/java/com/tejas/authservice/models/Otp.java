package com.tejas.authservice.models;

import java.time.LocalDateTime;

import com.tejas.bankingcommon.dto.OtpType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
@Builder
@Table(name="otp_entries")
public class Otp {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@NotNull
	private String otpHash;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	private OtpType type;
	
	@NotNull
	private String referenceId;
	
	@NotNull
	private LocalDateTime createdAt;
	
	@NotNull
	private LocalDateTime expiresAt;
	
	@NotNull
	private int attempts;
	
	@NotNull
	private int maxAttempts;
	
	@NotNull
	private Boolean used;
}
