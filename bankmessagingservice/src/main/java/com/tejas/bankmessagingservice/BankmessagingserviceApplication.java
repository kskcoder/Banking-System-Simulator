package com.tejas.bankmessagingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BankmessagingserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankmessagingserviceApplication.class, args);
	}

}
