package pl.cyfronet.rimrock.providers;

import org.globus.gsi.CredentialException;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import pl.cyfronet.rimrock.errors.BadCredentialsException;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

public class ProxyAuthenticationProvider implements AuthenticationProvider {
	private static final Logger log = LoggerFactory.getLogger(ProxyAuthenticationProvider.class);
	
	@Autowired private ProxyHelper proxyHelper;
	
	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		String credentials = (String) authentication.getCredentials();
		log.debug("Authenticating user with login {} and credentials {}", authentication.getName(),
				credentials);
		
		PreAuthenticatedAuthenticationToken result = null;
		
		try {
			proxyHelper.verify(credentials);
		} catch (CredentialException e) {
			log.warn("Bad credentials sent", e);
			
			throw new BadCredentialsException("Bad credentials provided: " + e.getMessage(), e);
		}
		
		
		result = new PreAuthenticatedAuthenticationToken(authentication.getName(), authentication.getCredentials());
		
		
		MDC.put("userLogin", authentication.getName());
		
		return result;
	}

	@Override
	public boolean supports(Class<?> c) {
		return c.isAssignableFrom(PreAuthenticatedAuthenticationToken.class);
	}
}