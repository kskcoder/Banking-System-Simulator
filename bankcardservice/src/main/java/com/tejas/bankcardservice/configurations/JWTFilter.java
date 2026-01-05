package com.tejas.bankcardservice.configurations;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tejas.bankcardservice.services.JWTService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class JWTFilter extends OncePerRequestFilter{
	
	@Value("${cardSecretKey}")
	private String cardSecretKey;
	
	@Value("${paymentInternalSecretKey}")
	private String paymentInternalSecretKey;
	
	@Value("${interServiceSecretKey}")
	private String interServiceSecretKey;
	
	@Autowired
	JWTService jwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		
		if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
			filterChain.doFilter(request, response);
			return;
		}
		
		String token = null;
		String userId = null;
		String role = null;
		String internalAuthKey = request.getHeader("X-Internal-Auth");
		
		if (internalAuthKey != null && 
		    (cardSecretKey.equals(internalAuthKey) || 
		     paymentInternalSecretKey.equals(internalAuthKey) || 
		     interServiceSecretKey.equals(internalAuthKey))) {
			role = request.getHeader("X-User-Role");
			userId = request.getHeader("X-User-Id");
			
			UsernamePasswordAuthenticationToken authToken =
			        new UsernamePasswordAuthenticationToken(userId, null,
			            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
			SecurityContextHolder.getContext().setAuthentication(authToken);
		} else {
			String authHeader = request.getHeader("Authorization");
			
			if (authHeader != null && authHeader.startsWith ("Bearer ")) {
				token = authHeader.substring(7);
			
				try {
					userId = jwtService.extractUserId(token);					
					
					if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
						if (jwtService.validateToken(token)) {
						    Claims claims = jwtService.extractAllClaims(token);
						    role = claims.get("role", String.class);

						    UsernamePasswordAuthenticationToken authToken =
						        new UsernamePasswordAuthenticationToken(userId, null,
						            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
						    SecurityContextHolder.getContext().setAuthentication(authToken);
						}
					} else {
						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	                    response.getWriter().write("Invalid or expired token");
	                    return;
					}
					
				} catch (Exception e) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	                response.getWriter().write("Invalid or expired token");
	                return;
				}
			} else {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	            response.getWriter().write("Authorization header missing or malformed");
	            return;
			}
		}
		filterChain.doFilter(request, response);
	}	
}
