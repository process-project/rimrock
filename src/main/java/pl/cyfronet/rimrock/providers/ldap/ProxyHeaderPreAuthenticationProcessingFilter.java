package pl.cyfronet.rimrock.providers.ldap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import pl.cyfronet.rimrock.gsi.ProxyHelper;

public class ProxyHeaderPreAuthenticationProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {
	private static final Logger log = LoggerFactory.getLogger(ProxyHeaderPreAuthenticationProcessingFilter.class);
	
	@Autowired private ProxyHelper proxyHelper;
	
	public ProxyHeaderPreAuthenticationProcessingFilter(AuthenticationManager authenticationManager) {
		setAuthenticationManager(authenticationManager);
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String proxyValue = request.getHeader("PROXY");
		
		if(proxyValue != null) {
			try {
				return proxyHelper.getUserLogin(proxyHelper.decodeProxy(proxyValue));
			} catch(Throwable e) {
				log.warn("Could not properly process proxy value", e);
				//ignoring - null will be returned
			}
		}
		
		return null;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		String proxyValue = request.getHeader("PROXY");
		
		if(proxyValue != null) {
			try {
				return proxyHelper.decodeProxy(proxyValue);
			} catch(Throwable e) {
				log.warn("Could not properly process proxy value", e);
				//ignoring - null will be returned
			}
		}
		
		return null;
	}
}