package com.tejas.bankauthservice.implementations;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tejas.bankauthservice.models.User;

public class UserPrincipal implements UserDetails{
	private String username;
	private String password;
	private String role;

	public UserPrincipal(User user) {
		super();
		this.username = user.getUsername();
		this.password = user.getPassword();
		this.role = user.getRole() != null ? user.getRole().toString() : "USER";
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singleton(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getPassword() {	
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
