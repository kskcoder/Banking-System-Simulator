package com.tejas.accountservice.models;

import com.tejas.accountservice.enums.AccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name="accounts")
@Data
public class Account {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	private long userid;
	private String accountnumber;
	
	@Column(name="accounttype")
	@Enumerated(EnumType.STRING)
	private AccountType accountType;
	private double balance;
}
