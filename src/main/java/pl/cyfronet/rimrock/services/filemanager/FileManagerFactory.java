package pl.cyfronet.rimrock.services.filemanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FileManagerFactory {

	@Value("${plgridData.url}")
	private String plgridDataUrl;
	
	@Autowired
	private RestTemplate restTemplate;
	
	public FileManager get(String proxyPayload) {
		return new FileManager(restTemplate, plgridDataUrl, proxyPayload);
	}
}