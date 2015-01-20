package pl.cyfronet.rimrock.providers.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class LdapAuthenticationProvider implements AuthenticationProvider {
	private static final Logger log = LoggerFactory.getLogger(LdapAuthenticationProvider.class);
	
	@Value("${ldap.rimrock.name}") private String ldapRimrockName;

	@Autowired private LdapTemplate ldapTemplate;
	
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

			Attribute attribute = attributes.get("plgridService");
			
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
		test();
		log.debug("Authenticating user with login {} and credentials {}", authentication.getName(), authentication.getCredentials());
		
		PlgridUser user = null;
		
		try {
			String dn = prepareDn(authentication.getName());
			log.info("Looking up {}", dn);
			user = (PlgridUser) ldapTemplate.lookup(dn, new PlgridUserAttributesMapper());
		} catch(NameNotFoundException e) {
			throw new MissingUserAuthenticationException("User " + authentication.getName() + " not found in LDAP");
		} catch (Exception e) {
			log.warn("Something went wrong with the LDAP lookup procedure while authorizing user " + authentication.getName(), e);
			
			throw new MissingUserAuthenticationException("User " + authentication.getName() + " not found in LDAP");
		}
		
		if(user == null) {
			throw new MissingUserAuthenticationException("User " + authentication.getName() + " not found in LDAP");
		}
		
		if(user.getServices().contains(ldapRimrockName)) {
			return new PreAuthenticatedAuthenticationToken(authentication.getName(), authentication.getCredentials());
		} else {
			throw new UserNotSignedForServiceAuthenticationException("User " + authentication.getName() + " found in LDAP but is not signed up for " + ldapRimrockName);
		}
	}

	private void test() {
		List result = ldapTemplate.search("", "(objectclass=person)", new AttributesMapper() {
			@Override
			public Object mapFromAttributes(Attributes attributes) throws NamingException {
				for(NamingEnumeration<String> e = attributes.getIDs(); e.hasMoreElements();) {
					System.out.println(e.next());
				}
				
				return attributes.get("uid").get();
			}
		});
		log.info(result.toString());
	}

	@Override
	public boolean supports(Class<?> c) {
		return c.isAssignableFrom(PreAuthenticatedAuthenticationToken.class);
	}
	
	private String prepareDn(String login) {
		return "uid=" + login + ",ou=People";
	}
}