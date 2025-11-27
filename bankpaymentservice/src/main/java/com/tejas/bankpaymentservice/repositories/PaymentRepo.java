package com.tejas.bankpaymentservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tejas.bankpaymentservice.models.Payment;

public interface PaymentRepo extends JpaRepository<Payment, Long>{
	public abstract boolean existsById(Long id);
}
