package com.tejas.authservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tejas.authservice.implementations.UserPrincipal;
import com.tejas.authservice.models.User;
import com.tejas.authservice.repositories.AuthRepo;

@Service
public class AuthUserDetailsService implements UserDetailsService{
	@Autowired
	private AuthRepo repo;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = repo.getByUsername(username).orElse(null);
		
		if (user == null)
			throw new UsernameNotFoundException("User not found! 404");
		
		
		return new UserPrincipal(user);	
		
	}

}
