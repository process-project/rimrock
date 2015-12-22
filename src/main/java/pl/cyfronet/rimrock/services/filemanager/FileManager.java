package pl.cyfronet.rimrock.services.filemanager;

import java.io.File;
import java.util.Arrays;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class FileManager {

	private String proxyPayload;
	private String plgridDataUrl;

	private static final Logger log = LoggerFactory.getLogger(FileManager.class);
	
	RestTemplate restTemplate;
	
	public FileManager(RestTemplate restTemplate, String plgridDataUrl, String proxyPayload) {
		this.restTemplate = restTemplate;
		this.plgridDataUrl = plgridDataUrl;
		this.proxyPayload = proxyPayload;		
	}

	public void cp(String filePath, Resource file) throws FileManagerException {
		MultiValueMap<String, Object> values = getFormDataWithProxyAndLang();		
		values.add("file", getFileEntity(filePath, file));
		values.add("recursive", true);
		
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(values, getHeders());
		log.debug("REQUEST: {}", request.toString());
		try {
			String fileDir = getFileDir(filePath);
			log.debug("uploadUrl: {}", uploadUrl(fileDir));
			restTemplate.postForEntity(uploadUrl(fileDir), request, null);
		} catch(HttpClientErrorException e) {
			throw new FileManagerException(e.getResponseBodyAsString());
		}
	}
	
	public void rm(String filePath) throws FileManagerException {
		rm(filePath, getFormDataWithProxyAndLang());
	}
	
	public void rmDir(String path) throws FileManagerException {
		MultiValueMap<String, Object> values = getFormDataWithProxyAndLang();
		values.add("is_dir", true);
		
		rm(path, values);
	}
	
	private void rm(String path, MultiValueMap<String, Object> values) throws FileManagerException {
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(values, getHeders());
		try {
			restTemplate.exchange(rmUrl(path), HttpMethod.DELETE, request, String.class);
		} catch(HttpClientErrorException e) {		
			throw new FileManagerException(e.getResponseBodyAsString());
		}
	}
	
	private MultiValueMap<String, Object> getFormDataWithProxyAndLang() {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("locale", "en");
		
		return values;
	}

	private HttpHeaders getHeders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.setAccept(Arrays.asList(MediaType.ALL));
		headers.add("PROXY", Base64.getEncoder().encodeToString(proxyPayload.getBytes()));
		
		return headers;
	}
	
	private HttpEntity<Resource> getFileEntity(String filePath, Resource file) {
		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		fileHeaders.setContentDispositionFormData("file", getFileName(filePath));
		
		return new HttpEntity<>(file, fileHeaders);
	}
	
	private String getFileName(String filePath) {
		File f = new File(filePath);
		return f.getName();		
	}
	
	private String getFileDir(String filePath) {
		File f = new File(filePath);
		return f.getParent();
	}
	
	private String uploadUrl(String filePath) {
		return plgdataUrl("upload", filePath);
	}
	
	private String rmUrl(String path) {
		return plgdataUrl("remove", path);
	}
	
	private String plgdataUrl(String action, String path) {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(plgridDataUrl);
		uriBuilder.path("/" + action + "/").path(path);
		return uriBuilder.build().toUriString();
	}
}