package pl.cyfronet.rimrock;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(merge = false, locations = "classpath:monitoring-accounts-mapping.properties")
public class MonitoringAccounts {
	private Map<String, String> mapping;
	
	public MonitoringAccounts() {
		mapping = new HashMap<>();
	}

	public Map<String, String> getMapping() {
		return mapping;
	}

	public void setMapping(Map<String, String> mapping) {
		this.mapping = mapping;
	}
}