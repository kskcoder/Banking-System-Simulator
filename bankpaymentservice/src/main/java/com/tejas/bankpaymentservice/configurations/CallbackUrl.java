package com.tejas.bankpaymentservice.configurations;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(value="payment")
@Data
public class CallbackUrl {
	Map<String, String> callbackUrl;
}
