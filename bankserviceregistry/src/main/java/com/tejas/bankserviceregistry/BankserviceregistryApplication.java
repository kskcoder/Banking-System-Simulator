package com.tejas.bankserviceregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class BankserviceregistryApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankserviceregistryApplication.class, args);
	}

}