package pl.cyfronet.rimrock;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.catalina.connector.Connector;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@SpringBootApplication
@EnableConfigurationProperties
public class RimrockApplication extends WebMvcConfigurerAdapter {
	private static final Logger log = LoggerFactory.getLogger(RimrockApplication.class);
	
	@Autowired
	private RequestLoggingInterceptor loggingInterceptor;
	
	@Value("${max.header.size.bytes}")
	private int maxHeaderSizeBytes;

	public static void main(String[] args) {
		new SpringApplicationBuilder(RimrockApplication.class).run(args);
		log.info("rimrock application successfully started");
	}
	
	@Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(loggingInterceptor)
        		.addPathPatterns("/api/**");
    }

	@Bean
	RestTemplate getRestTemplate() {
		return new RestTemplate();
	}
	
	@Bean
    LocaleResolver localeResolver() {
		CookieLocaleResolver clr = new CookieLocaleResolver();
        clr.setDefaultLocale(Locale.US);
        clr.setCookieName("rimrock-lang");
        
        return clr;
    }
 
    @Bean
    LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        
        return lci;
    }
    
	/*
	 * Used to reload message files at runtime during development.
	 */
	@Bean
	@Profile("local")
	MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames("classpath:messages");
		messageSource.setCacheSeconds(1);

		return messageSource;
	}
	
	@Bean
	EmbeddedServletContainerCustomizer containerCustomizer() {
	    return new EmbeddedServletContainerCustomizer() {
			@Override
			public void customize(ConfigurableEmbeddedServletContainer container) {
				if(container instanceof TomcatEmbeddedServletContainerFactory) {
					TomcatEmbeddedServletContainerFactory factory = (TomcatEmbeddedServletContainerFactory) container;
					factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
						@Override
						public void customize(Connector connector) {
							connector.setAttribute("maxHttpHeaderSize", "" + maxHeaderSizeBytes);
						}
					});
				}
			}
		};
	}
	
	@Bean
	FilterRegistrationBean mdcFilter() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new Filter() {
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				//not needed
			}
			
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				//doing the whole chain processing: userLogin value will be set by
				//the pl.cyfronet.rimrock.providers.ldap.LdapAuthenticationProvider.authenticate(Authentication) method
				chain.doFilter(request, response);
				
				//cleaning the set userLogin value
				MDC.remove("userLogin");
			}
			
			@Override
			public void destroy() {
				//not needed
			}
		});
		registration.addUrlPatterns("/api/*");
		
		return registration;
	}
}