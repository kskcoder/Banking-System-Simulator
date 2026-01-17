package com.tejas.bankmessagingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import com.tejas.bankmessagingservice.configurations.MailProperties;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(exclude = {MailSenderAutoConfiguration.class})
@EnableDiscoveryClient
@EnableConfigurationProperties
@Slf4j
public class BankmessagingserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankmessagingserviceApplication.class, args);
	}
	
	@Bean
	@ConfigurationProperties(prefix = "spring.mail", ignoreUnknownFields = true, ignoreInvalidFields = true)
	MailProperties mailProperties() {
		try {
			return new MailProperties();
		} catch (Exception e) {
			log.warn("Failed to create MailProperties bean due to configuration issues: {}", e.getMessage());
			log.warn("Email functionality will be disabled. Email messages will be logged to console.");
			return new MailProperties();
		}
	}

}