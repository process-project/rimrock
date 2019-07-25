package pl.cyfronet.rimrock.errors;

import static org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import java.util.Map;

import org.springframework.boot.autoconfigure.web.DefaultErrorAttributes;
import org.springframework.web.context.request.RequestAttributes;

public class CustomErrorAttributes extends DefaultErrorAttributes {
	@Override
	public Map<String, Object> getErrorAttributes(RequestAttributes requestAttributes, boolean includeStackTrace) {
		Map<String, Object> errorAttributes = super.getErrorAttributes(requestAttributes, includeStackTrace);
		addAuthenticationError(requestAttributes, errorAttributes);
		
		return errorAttributes;
	}

	private void addAuthenticationError(RequestAttributes requestAttributes, Map<String, Object> errorAttributes) {
		Throwable error = (Throwable) requestAttributes.getAttribute(AUTHENTICATION_EXCEPTION, SCOPE_REQUEST);
		
		if(error != null) {
			errorAttributes.put("error_message", error.getMessage());
		}
	}
}