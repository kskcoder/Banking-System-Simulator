package com.tejas.bankcardservice.model;

import java.time.LocalDateTime;


import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
	@Column(length=7)
	private String expiryDate;
	
	@NotNull
	private double cardLimit;
	
	@NotNull
	private boolean isBlocked;
	
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
		this.isBlocked = false;
	}
	
	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
