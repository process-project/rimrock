package pl.cyfronet.rimrock;

import java.util.Locale;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import pl.cyfronet.rimrock.gridworkerapi.service.GridWorkerService;

@SpringBootApplication
@EnableConfigurationProperties
public class RimrockApplication extends WebMvcConfigurerAdapter {
	private static final Logger log = LoggerFactory.getLogger(RimrockApplication.class);
	
	@Value("${max.header.size.bytes}") private int maxHeaderSizeBytes;
	@Value("${jsaga.jar.version}") private String jSagaGridWorkerVersion;
	@Value("${qcg.jar.version}") private String qcgGridWorkerVersion;

	public static void main(String[] args) {
		new SpringApplicationBuilder(RimrockApplication.class).run(args);
		log.info("rimrock application successfully started");
	}
	
	@Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
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
	@Qualifier("jsaga")
	GridWorkerServer jSagaGridWorker() {
		return new GridWorkerServer("rimrock-jsaga", jSagaGridWorkerVersion);
	}
	
	@Bean
	@Qualifier("jsaga")
	GridWorkerService jSagaGridWorkerService(@Qualifier("jsaga") GridWorkerServer gridWorkerServer) throws InterruptedException {
		RmiProxyFactoryBean gridWorkerServiceFactory = new RmiProxyFactoryBean();
		gridWorkerServiceFactory.setServiceUrl("rmi://localhost:" + gridWorkerServer.getRegistryPort() + "/jSagaGridWorkerService");
		gridWorkerServiceFactory.setServiceInterface(GridWorkerService.class);
		
		while(true) {
			try {
				gridWorkerServiceFactory.afterPropertiesSet();
				
				break;
			} catch(Exception e) {
				log.info("Waiting for the JSaga grid worker server...");
			}
			
			Thread.sleep(500);
		}
		
		return (GridWorkerService) gridWorkerServiceFactory.getObject();
	}
	
	@Bean
	@Qualifier("qcg")
	GridWorkerServer qcgGridWorker() {
		return new GridWorkerServer("rimrock-qcg", jSagaGridWorkerVersion);
	}
	
	@Bean
	@Qualifier("qcg")
	GridWorkerService qcgGridWorkerService(@Qualifier("qcg") GridWorkerServer gridWorkerServer) throws InterruptedException {
		RmiProxyFactoryBean gridWorkerServiceFactory = new RmiProxyFactoryBean();
		gridWorkerServiceFactory.setServiceUrl("rmi://localhost:" + gridWorkerServer.getRegistryPort() + "/qcgGridWorkerService");
		gridWorkerServiceFactory.setServiceInterface(GridWorkerService.class);
		
		while(true) {
			try {
				gridWorkerServiceFactory.afterPropertiesSet();
				
				break;
			} catch(Exception e) {
				log.info("Waiting for the QCG grid worker server...");
			}
			
			Thread.sleep(500);
		}
		
		return (GridWorkerService) gridWorkerServiceFactory.getObject();
	}
}