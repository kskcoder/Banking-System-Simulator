package com.tejas.configserverservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigserverserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigserverserviceApplication.class, args);
	}

}
