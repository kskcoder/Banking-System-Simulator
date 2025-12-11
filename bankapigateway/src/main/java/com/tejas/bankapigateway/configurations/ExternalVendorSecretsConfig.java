package com.tejas.bankapigateway.configurations;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(value="gateway")
@Data
public class ExternalVendorSecretsConfig {
	private Map<String, String> vendorKey;
}
