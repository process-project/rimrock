package pl.cyfronet.rimrock.controllers.exceptions;

import org.springframework.web.client.RestClientException;

public class ResourceNotFoundException extends RestClientException {
	private static final long serialVersionUID = -32013440000517718L;

	public ResourceNotFoundException(String msg) {
		super(msg);
	}
}