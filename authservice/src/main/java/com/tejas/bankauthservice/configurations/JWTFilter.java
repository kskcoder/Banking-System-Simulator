package com.tejas.bankauthservice.configurations;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		String token = null;
		String userId = null;
		
		if (authHeader != null && authHeader.startsWith ("Bearer ")) {
			token = authHeader.substring(7);
			userId = jwtService.extractUserId(token);
		}
		
		if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			User user = repo.getById(Long.parseLong(userId))
				.orElseThrow(() -> 
					new NotFoundException("User not found")
				);
			
			UserDetails userDetails = context.getBean(AuthUserDetailsService.class).loadUserByUsername(user.getUsername());
			
			if (jwtService.validateToken(token, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}
		
		filterChain.doFilter(request, response);
		
	}

}
