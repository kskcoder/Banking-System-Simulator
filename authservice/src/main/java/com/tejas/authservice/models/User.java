package com.tejas.authservice.models;

import java.time.LocalDateTime;

import com.tejas.bankingcommon.enums.UserType;

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
@Data
@Table(name="usercred")
public class User {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique=true)
	private String username;
	private String email;
	private String password;
	private String phone;
	
	@Enumerated(EnumType.STRING)
	private UserType role; 
	
	@Column(name="created_at")
	private LocalDateTime createdAt; 
	
	@Column(name="updated_at")
	private LocalDateTime updatedAt;
}
