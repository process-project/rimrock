package pl.cyfronet.rimrock.providers.ldap;

import org.springframework.security.core.AuthenticationException;

public class BadCredentialsException extends AuthenticationException {
	private static final long serialVersionUID = -7667826438309608927L;
	
	public BadCredentialsException(String msg, Throwable t) {
		super(msg, t);
	}
}