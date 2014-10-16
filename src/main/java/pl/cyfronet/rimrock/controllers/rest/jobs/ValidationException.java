package pl.cyfronet.rimrock.controllers.rest.jobs;

import org.springframework.validation.BindingResult;

import pl.cyfronet.rimrock.controllers.rest.RestHelper;

public class ValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public ValidationException(String message) {
		super(message);
	}
	
	public ValidationException(BindingResult errors) {
		this(RestHelper.convertErrors(errors));
	}
}
