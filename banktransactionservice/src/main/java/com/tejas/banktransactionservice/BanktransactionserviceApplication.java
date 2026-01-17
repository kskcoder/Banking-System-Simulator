package com.tejas.banktransactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
@EnableJpaRepositories(basePackages = "com.tejas.banktransactionservice.repositories")
@SecurityScheme(
	    name = "bearerAuth",
	    type = SecuritySchemeType.HTTP,
	    scheme = "bearer",
	    bearerFormat = "JWT"
	)
public class BanktransactionserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BanktransactionserviceApplication.class, args);
	}

}