package pl.cyfronet.rimrock.providers.ldap;

import org.springframework.security.core.AuthenticationException;

public class UserNotSignedForServiceAuthenticationException extends AuthenticationException {
	private static final long serialVersionUID = 1171735240970396877L;

	public UserNotSignedForServiceAuthenticationException(String msg) {
		super(msg);
	}
}