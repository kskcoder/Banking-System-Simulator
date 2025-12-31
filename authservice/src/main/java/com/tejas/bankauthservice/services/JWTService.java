package com.tejas.bankauthservice.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.tejas.bankauthservice.implementations.UserPrincipal;
import com.tejas.bankingcommon.enums.UserType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JWTService {
	
	@Value("${jwt.secret}")
	private String secretKey;
	
	public String generateToken(Long userId, UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		
		String role = userDetails.getAuthorities().stream()
		        .findFirst()
		        .map(GrantedAuthority::getAuthority)
		        .orElse(UserType.USER.toString())
		        .replace("ROLE_", "");
	
		claims.put("role", role);
		
		return Jwts.builder()
				.claims(claims)
				.subject(String.valueOf(userId))
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
				.signWith(getKey())
				.compact();
	}	
	
	private SecretKey getKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
	
	public String extractUserId(String token) {
		return extractClaim(token, Claims::getSubject);
	}

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
	
	public boolean validateToken(String token, UserDetails userDetails) {
    	final String tokenUserId = extractUserId(token);
    	if (userDetails instanceof UserPrincipal) {
    		UserPrincipal principal = (UserPrincipal) userDetails;
    		return (tokenUserId.equals(String.valueOf(principal.getUserId())) && !isTokenExpired(token));
    	}
    	return false;
	}
	
    private boolean isTokenExpired(String token) {
    	 return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
