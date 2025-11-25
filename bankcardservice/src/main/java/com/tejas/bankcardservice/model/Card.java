package com.tejas.bankcardservice.model;

import java.time.LocalDateTime;

import com.tejas.bankingcommon.enums.AccountCardStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name="cards")
@Builder
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Card {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	
	@Column(unique = true)
	@NotNull
	private String cardNumber;
	
	@NotNull
	private String cvv;
	
	@NotNull
	private String lastDigits;
	
	@NotNull
	@Column(length=7)
	private String expiryDate;
	
	@NotNull
	private double cardLimit;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	private AccountCardStatus status;
	
	@NotNull
	private long accountId;
	
	@NotNull
	private LocalDateTime createdAt;
	
	@NotNull
	private LocalDateTime updatedAt;
	
	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		this.status = AccountCardStatus.INACTIVE;
	}
	
	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
