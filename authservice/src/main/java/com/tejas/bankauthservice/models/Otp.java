package com.tejas.bankauthservice.models;

import java.time.LocalDateTime;

import com.tejas.bankingcommon.dto.MessageType;
import com.tejas.bankingcommon.dto.OtpStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="otp_entries")
public class Otp {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@NotNull
	private String otpHash;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	private MessageType type;
	
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
	@Enumerated(EnumType.STRING)
	private OtpStatus status;
}
