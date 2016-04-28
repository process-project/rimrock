package pl.cyfronet.rimrock.controllers.rest;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.LOCKED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
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
import org.springframework.web.client.ResourceAccessException;

import com.jcraft.jsch.JSchException;

import pl.cyfronet.rimrock.controllers.exceptions.ResourceNotFoundException;
import pl.cyfronet.rimrock.controllers.rest.jobs.ValidationException;
import pl.cyfronet.rimrock.controllers.rest.proxygeneration.BanException;
import pl.cyfronet.rimrock.controllers.rest.proxygeneration.ProxyGenerationException;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.gsissh.RunException;

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
		
		msg = String.format("%s. Make sure that your proxy is a valid SimpleCA certificate.",
				e.getMessage());

		return new ResponseEntity<ErrorResponse>(new ErrorResponse(msg), UNAUTHORIZED);
	}
	
	@ExceptionHandler({ FileManagerException.class, GSSException.class, IOException.class,
		InterruptedException.class, JSchException.class })
	public ResponseEntity<ErrorResponse> handleRunCmdError(Exception e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()),
				INTERNAL_SERVER_ERROR);
	}
	
	@ExceptionHandler(RunException.class)
	public ResponseEntity<ErrorResponse> handleRunError(RunException e) {
		log.error("Global error intercepted", e);
		log.error("Run exception details: \n\texit code: {}, \n\terror output: {}\n\t"
				+ "standard output: {}", e.getExitCode(), e.getError(), e.getOutput());
		
		HttpStatus status = e.isTimeoutOccured() ? REQUEST_TIMEOUT : INTERNAL_SERVER_ERROR;
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e), status);
	}
	
	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ErrorResponse> handleValidationError(ValidationException e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(-1, e.getMessage()),
				UNPROCESSABLE_ENTITY);
	}
	
	@ExceptionHandler(ResourceAccessException.class)
	public ResponseEntity<ErrorResponse> handleAccessException(ResourceAccessException e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), FORBIDDEN);
	}
	
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleAccessException(ResourceNotFoundException e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), NOT_FOUND);
	}
	
	@ExceptionHandler(BanException.class)
	public ResponseEntity<ErrorResponse> handleBanException(BanException e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()), LOCKED);
	}
	
	@ExceptionHandler(ProxyGenerationException.class)
	public ResponseEntity<ErrorResponse> handleProxyGenerationException(
			ProxyGenerationException e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()),
				UNPROCESSABLE_ENTITY);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleError(Exception e) {
		log.error("Global error intercepted", e);
		
		return new ResponseEntity<ErrorResponse>(new ErrorResponse(e.getMessage()),
				INTERNAL_SERVER_ERROR);
	}
}