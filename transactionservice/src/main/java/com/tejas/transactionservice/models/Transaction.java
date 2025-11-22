package com.tejas.transactionservice.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name="transactions")
@Data
public class Transaction {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	private String fromAccount;
	private String toAccount;
	private double amount;
	private String status;
	@Column(name="created_at")
	private LocalDateTime createdAt; 
	
	@Column(name="updated_at")
	private LocalDateTime updatedAt;
}
