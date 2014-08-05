package pl.cyfronet.rimrock.services.filemanager;

import java.util.Arrays;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class FileManager {

	private String proxyPayload;
	private String plgridDataUrl;
	
	RestTemplate restTemplate;
	
	public FileManager(RestTemplate restTemplate, String plgridDataUrl, String proxyPayload) {
		this.restTemplate = restTemplate;
		this.plgridDataUrl = plgridDataUrl;
		this.proxyPayload = proxyPayload;		
	}

	public void copyFile(String targetDir, Resource file) throws FileManagerException {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();		
		
		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		
		values.add("proxy", proxyPayload);
		values.add("file", file);
		values.add("locale", "en");
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.setAccept(Arrays.asList(MediaType.ALL));
		
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(values, headers);
		try {
			restTemplate.postForEntity(postUrl(targetDir), request, null);
		} catch(HttpClientErrorException e) {
			throw new FileManagerException(e.getResponseBodyAsString());
		}
	}
	
	private String postUrl(String filePath) {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(plgridDataUrl);
		uriBuilder.path("/upload/").path(filePath);
		return uriBuilder.build().toUriString();
	}
}
