package pl.cyfronet.rimrock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import pl.cyfronet.rimrock.errors.CustomErrorAttributes;
import pl.cyfronet.rimrock.filters.ProxyHeaderPreAuthenticationProcessingFilter;
import pl.cyfronet.rimrock.providers.ProxyAuthenticationProvider;

@Configuration
@EnableWebSecurity
public class RimrockSecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${unsecure.api.resources}")	private String unsecureApiResources;
	
	@Autowired
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(proxyAuthenticationProvider());
	}

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

	@Bean
	protected ProxyHeaderPreAuthenticationProcessingFilter proxyHeaderfilter(AuthenticationManager authenticationManager) {
		return new ProxyHeaderPreAuthenticationProcessingFilter(authenticationManager);
	}

	@Bean
	protected ProxyAuthenticationProvider proxyAuthenticationProvider() {
		return new ProxyAuthenticationProvider();
	}
	
	@Bean
	protected ErrorAttributes errorAttributes() {
		return new CustomErrorAttributes();
	}
}