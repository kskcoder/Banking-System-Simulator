package com.tejas.bankauthservice.configurations;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tejas.bankauthservice.models.User;
import com.tejas.bankauthservice.repositories.AuthRepo;
import com.tejas.bankauthservice.services.AuthUserDetailsService;
import com.tejas.bankauthservice.services.JWTService;
import com.tejas.bankingcommon.exceptions.NotFoundException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JWTFilter extends OncePerRequestFilter {
	
	@Autowired
	private JWTService jwtService; 
	
	@Autowired
	private AuthRepo repo; 
	
	@Autowired
	ApplicationContext context;
	
	@Value("${interServiceSecretKey}")
	private String interServiceSecretKey;
	
	@Value("${authSecretKey}")
	private String authSecretKey;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		
		System.out.println("Auth Service JWTFilter - Path: " + path);
		
		if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		String token = null;
		String userId = null;
		String role = null;
		String internalAuthKey = request.getHeader("X-Internal-Auth");
		
		System.out.println("Auth Service JWTFilter - X-Internal-Auth header: " + (internalAuthKey != null ? "present (length: " + internalAuthKey.length() + ")" : "null"));
		System.out.println("Auth Service JWTFilter - interServiceSecretKey: " + (interServiceSecretKey != null ? "present (length: " + interServiceSecretKey.length() + ")" : "null"));
		System.out.println("Auth Service JWTFilter - Keys match: " + (internalAuthKey != null && interServiceSecretKey != null && interServiceSecretKey.equals(internalAuthKey)));
		
		System.out.println(internalAuthKey);
		System.out.println(authSecretKey);
		
		// Handle inter-service authentication
		if (internalAuthKey != null && interServiceSecretKey.equals(internalAuthKey) || interServiceSecretKey.equals(authSecretKey)) {
			role = request.getHeader("X-User-Role");
			userId = request.getHeader("X-User-Id");
			
			System.out.println("Auth Service JWTFilter - Internal auth matched. Role: " + role + ", UserId: " + userId);
			
			if (role != null && userId != null) {
				UsernamePasswordAuthenticationToken authToken =
				        new UsernamePasswordAuthenticationToken(userId, null,
				            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
				SecurityContextHolder.getContext().setAuthentication(authToken);
				System.out.println("Auth Service JWTFilter - Authentication set successfully");
			} else {
				System.out.println("Auth Service JWTFilter - Role or UserId is null, authentication not set");
			}
		} else {
			System.out.println("Auth Service JWTFilter - Internal auth key mismatch or missing, trying JWT authentication");
			// Handle JWT token authentication
			String authHeader = request.getHeader("Authorization");
			
			if (authHeader != null && authHeader.startsWith ("Bearer ")) {
				token = authHeader.substring(7);
				userId = jwtService.extractUserId(token);
			}
			
			if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				User user = repo.findById(Long.parseLong(userId))
					.orElseThrow(() -> 
						new NotFoundException("User not found")
					);
				
				String username = user.getUsername();
				
				UserDetails userDetails = context.getBean(AuthUserDetailsService.class).loadUserByUsername(username);
				
				if (jwtService.validateToken(token, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId, null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		}
		
		filterChain.doFilter(request, response);
		
	}

}
