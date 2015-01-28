package pl.cyfronet.rimrock.providers.ldap;

import org.springframework.security.core.AuthenticationException;

public class MissingUserAuthenticationException extends AuthenticationException {
	private static final long serialVersionUID = -5792873312257035722L;

	public MissingUserAuthenticationException(String msg) {
		super(msg);
	}
}