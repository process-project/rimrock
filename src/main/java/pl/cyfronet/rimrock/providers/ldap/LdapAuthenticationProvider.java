package pl.cyfronet.rimrock.providers.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.globus.gsi.CredentialException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import pl.cyfronet.rimrock.gsi.ProxyHelper;

public class LdapAuthenticationProvider implements AuthenticationProvider {
	private static final Logger log = LoggerFactory.getLogger(LdapAuthenticationProvider.class);
	
	@Value("${ldap.rimrock.name}") private String ldapRimrockName;
	@Value("${ldap.integration.enabled}") private boolean ldapEbabled;
	@Value("${ldap.service.field.name}") private String ldapServiceFieldName;

	@Autowired private LdapTemplate ldapTemplate;
	@Autowired private ProxyHelper proxyHelper;
	
	private class PlgridUser {
		private List<String> services;

		public List<String> getServices() {
			return services;
		}

		public void setServices(List<String> services) {
			this.services = services;
		}
	}
	
	private class PlgridUserAttributesMapper implements AttributesMapper {
		@Override
		public Object mapFromAttributes(Attributes attributes) throws NamingException {
			PlgridUser user = new PlgridUser();
			List<String> services = new ArrayList<>();
			user.setServices(services);

			Attribute attribute = attributes.get(ldapServiceFieldName);
			
			if(attribute != null && attribute.size() > 0) {
				for(int i = 0; i < attribute.size(); i++) {
					services.add((String) attribute.get(i));
				}
			}
			
			return user;
		}
	}
	
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		log.debug("Authenticating user with login {} and credentials {}", authentication.getName(), authentication.getCredentials());
		
		try {
			proxyHelper.verify((String) authentication.getCredentials());
		} catch (CredentialException e) {
			log.warn("Bad credentials sent", e);
			
			throw new BadCredentialsException("Bad credentials provided: " + e.getMessage(), e);
		}
		
		if(ldapEbabled) {
			PlgridUser user = null;
			
			try {
				String dn = prepareDn(authentication.getName());
				log.debug("LDAP authorization procedure started for user {} with dn {}", authentication.getName(), dn);
				user = (PlgridUser) ldapTemplate.lookup(dn, new PlgridUserAttributesMapper());
			} catch (Exception e) {
				log.warn("Something went wrong with the LDAP lookup procedure while authorizing user " + authentication.getName(), e);
			}
			
			if(user == null) {
				String message = "User " + authentication.getName() + " not found in LDAP";
				log.debug(message);
				
				throw new MissingUserAuthenticationException(message);
			}
			
			if(user.getServices().contains(ldapRimrockName)) {
				log.debug("LDAP authorization was successful for {}", authentication.getName());
				
				return new PreAuthenticatedAuthenticationToken(authentication.getName(), authentication.getCredentials());
			} else {
				throw new UserNotSignedForServiceAuthenticationException("User " + authentication.getName() + " found in LDAP but is not signed up for " + ldapRimrockName);
			}
		}
		
		return new PreAuthenticatedAuthenticationToken(authentication.getName(), authentication.getCredentials());
	}

	@Override
	public boolean supports(Class<?> c) {
		return c.isAssignableFrom(PreAuthenticatedAuthenticationToken.class);
	}
	
	private String prepareDn(String login) {
		return "uid=" + login + ",ou=People";
	}
}