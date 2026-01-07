package com.tejas.bankpaymentservice.models;

import java.time.LocalDateTime;

import com.tejas.bankingcommon.enums.PaymentStatus;
import com.tejas.bankingcommon.enums.PaymentType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name="payments")
@Builder
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Payment {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	
	private String vendorId;
	private String fromAccountNumber;
	private String toAccountNumber;
	private double amount;
	
	@Enumerated(EnumType.STRING)
	private PaymentType type;
	
	@Enumerated(EnumType.STRING)
	private PaymentStatus status;
	
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	@PrePersist
	public void atCreation() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	@PreUpdate
	public void atUpdation() {
		this.updatedAt = LocalDateTime.now();
	}
}
