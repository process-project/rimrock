package pl.cyfronet.rimrock.controllers.rest;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import pl.cyfronet.rimrock.controllers.rest.jobs.ValidationException;
import pl.cyfronet.rimrock.services.RunException;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;

import com.sshtools.j2ssh.util.InvalidStateException;

@ControllerAdvice
public class GlobalExceptionHandling {
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandling.class);
	
	@ExceptionHandler(CredentialException.class)
	public ResponseEntity<ErrorResponse> handleCredentialsError(CredentialException e) {
		log.error("Global error intercepted", e);
		
		String msg = e.getMessage(); 
				
		Pattern p = Pattern.compile("\\A[\\w\\.]*\\w?Exception: (.*)\\z");
		Matcher m = p.matcher(msg);
		
		if(m.find()) {
			msg = m.group(1);
		} 
		msg = String.format("%s. Make sure that your proxy is a valid SimpleCA certificate.", e.getMessage());

		return new ResponseEntity<ErrorResponse>(new ErrorResponse(msg), FORBIDDEN);
	}
	
	@ExceptionHandler({FileManagerException.class, 
		InvalidStateException.class, GSSException.class, 
		IOException.class, InterruptedException.class})
	public ResponseEntity<ErrorResponse> handleRunCmdError(Exception e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), INTERNAL_SERVER_ERROR);
	}
	
	@ExceptionHandler(RunException.class)
	public ResponseEntity<ErrorResponse> handleRunError(RunException e) {
		log.error("Global error intercepted", e);
		
		HttpStatus status = e.isTimeoutOccured() ? REQUEST_TIMEOUT : INTERNAL_SERVER_ERROR;
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e), status);
	}
	
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ErrorResponse> handleValidationError(ValidationException e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(-1, e.getMessage()), UNPROCESSABLE_ENTITY);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleError(Exception e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), INTERNAL_SERVER_ERROR);
	}
}