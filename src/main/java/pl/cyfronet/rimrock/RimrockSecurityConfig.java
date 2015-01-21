package pl.cyfronet.rimrock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.referral.ReferralInterceptor;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

import pl.cyfronet.rimrock.providers.ldap.LdapAuthenticationProvider;
import pl.cyfronet.rimrock.providers.ldap.ProxyHeaderPreAuthenticationProcessingFilter;

@Configuration
@EnableWebMvcSecurity
public class RimrockSecurityConfig extends WebSecurityConfigurerAdapter {
	private static final Logger log = LoggerFactory.getLogger(RimrockSecurityConfig.class);

	@Value("${unsecure.api.resources}")	private String unsecureApiResources;
	@Value("classpath:plgrid-minimal.ldif")	private Resource ldapData;

	private LdapServer localLdapServer;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.
			authorizeRequests().
				antMatchers(unsecureApiResources.split(",")).
					permitAll().
				antMatchers("/api/**").
					fullyAuthenticated().
				anyRequest().
					permitAll().
					and().
			addFilter(proxyHeaderfilter(authenticationManager())).
			sessionManagement().
				sessionCreationPolicy(SessionCreationPolicy.STATELESS).
				and().
			csrf().
				disable();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(ldapAuthenticationProvider());
	}

	@Bean
	protected ProxyHeaderPreAuthenticationProcessingFilter proxyHeaderfilter(AuthenticationManager authenticationManager) {
		return new ProxyHeaderPreAuthenticationProcessingFilter(authenticationManager);
	}

	@Bean
	protected LdapAuthenticationProvider ldapAuthenticationProvider() {
		return new LdapAuthenticationProvider();
	}

	@Bean
	@Profile("local")
	protected LdapTemplate ldapTemplate() throws Exception {
		int serverPort = startLocalLdapServer();

		DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource("ldap://127.0.0.1:" + serverPort
				+ "/dc=Cyfronet,dc=plgrid,dc=pl");
		contextSource.afterPropertiesSet();
		log.info("Embedded LDAP server started on port " + serverPort);

		return new LdapTemplate(contextSource);
	}


	@Bean
	@Profile("production")
	protected LdapTemplate productionLdapTemplate() throws Exception {
		return new LdapTemplate();
	}
	
	@PreDestroy
	private void stopLdapServer() throws Exception {
		log.info("Gracefully shutting down local LDAP server");
		
		if(localLdapServer != null && localLdapServer.isStarted()) {
			localLdapServer.getDirectoryService().shutdown();
			localLdapServer.stop();
		}
	}
	
	private int startLocalLdapServer() throws Exception, InvalidNameException, NamingException, IOException {
		int serverPort = 8081;
		String root = "dc=Cyfronet,dc=plgrid,dc=pl";
		DefaultDirectoryService service = new DefaultDirectoryService();
		List<Interceptor> list = new ArrayList<Interceptor>();
		list.add(new NormalizationInterceptor());
		list.add(new AuthenticationInterceptor());
		list.add(new ReferralInterceptor());
		list.add(new ExceptionInterceptor());
		list.add(new OperationalAttributeInterceptor());
		list.add(new SchemaInterceptor());
		list.add(new SubentryInterceptor());
		service.setInterceptors(list);
		
		JdbmPartition partition = new JdbmPartition();
		partition.setId("rootPartition");
		partition.setSuffix(root);
		service.addPartition(partition);
		service.setExitVmOnShutdown(false);
		service.setShutdownHookEnabled(false);
		service.getChangeLog().setEnabled(false);
		service.setDenormalizeOpAttrsEnabled(true);
		service.setWorkingDirectory(new File("/tmp/" + UUID.randomUUID().toString()));
		
		localLdapServer = new LdapServer();
		localLdapServer.setDirectoryService(service);
		localLdapServer.setTransports(new TcpTransport(serverPort));
		service.startup();
		localLdapServer.start();
		
		LdapDN dn = new LdapDN(root);
		String dc = root.substring(3, root.indexOf(','));
		ServerEntry entry = service.newEntry(dn);
		entry.add("objectClass", "top", "domain", "extensibleObject");
		entry.add("dc", dc);
		service.getAdminSession().add(entry);
		
		LdifFileLoader loader = new LdifFileLoader(service.getAdminSession(), ldapData.getFilename());
		loader.execute();
		
		return serverPort;
	}
}