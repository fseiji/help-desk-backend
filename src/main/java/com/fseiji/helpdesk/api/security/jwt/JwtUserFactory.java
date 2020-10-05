package com.fseiji.helpdesk.api.security.jwt;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fseiji.helpdesk.api.entity.User;
import com.fseiji.helpdesk.api.enums.ProfileEnum;

/**
 * Classe responsável por converter o User para User reconhecido pelo
 * SpringSecurity
 */
public class JwtUserFactory {

	private JwtUserFactory() {
	}

	// cria um JWTUser com base nos dados do User
	public static JwtUser create(User user) {
		return new JwtUser(user.getId(), user.getEmail(), user.getPassword(),
				mapToGrantedAuthorities(user.getProfile()));
	}

	// converte o perfil do User para o formato utilizado pelo SpringSecurity
	private static List<GrantedAuthority> mapToGrantedAuthorities(ProfileEnum profileEnum) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority(profileEnum.toString()));
		return authorities;
	}
}
