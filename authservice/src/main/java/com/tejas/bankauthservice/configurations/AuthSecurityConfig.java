package com.tejas.bankauthservice.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class AuthSecurityConfig {
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	JWTFilter jwtFilter;
	
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
	
	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(new BCryptPasswordEncoder());
		return provider;
	}
	
	@Bean
	SecurityFilterChain securityfilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/auth/login","/auth/loginuser", "/auth/signup").permitAll()
					.anyRequest().authenticated())
			.csrf(csrf -> csrf.disable())
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
			.logout(logout -> logout
					.logoutUrl("/auth/logout")
					.logoutSuccessUrl("/auth/login")
					.invalidateHttpSession(true)
					.clearAuthentication(true)
					.permitAll()
			);
		return http.build();
	}
}
