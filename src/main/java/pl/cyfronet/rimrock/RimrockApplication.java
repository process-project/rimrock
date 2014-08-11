package pl.cyfronet.rimrock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@EnableAutoConfiguration
@ComponentScan
public class RimrockApplication {
	private static final Logger log = LoggerFactory.getLogger(RimrockApplication.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(RimrockApplication.class).run(args);
		log.info("rimrock application successfully started");
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}
}