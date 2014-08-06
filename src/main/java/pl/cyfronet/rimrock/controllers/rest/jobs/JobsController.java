package pl.cyfronet.rimrock.controllers.rest.jobs;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import pl.cyfronet.rimrock.controllers.rest.RestHelper;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;

@Controller
public class JobsController {
	private static final Logger log = LoggerFactory.getLogger(JobsController.class);
	
	@Autowired private FileManagerFactory fileManagerFactory;
	
	@RequestMapping(value = "/api/jobs", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<SubmitResponse> submit(
			@RequestHeader(value = "PROXY", required = false) String proxy,
			@Valid @RequestBody SubmitRequest submitRequest, BindingResult errors) {
		log.debug("Processing run request {}", submitRequest);
		
		if(errors.hasErrors()) {
			return new ResponseEntity<SubmitResponse>(new SubmitResponse(null, RestHelper.convertErrors(errors), null), UNPROCESSABLE_ENTITY);
		}
		
		try {
			FileManager fileManager = fileManagerFactory.get(submitRequest.getProxy());
			fileManager.copyFile("/people/plgtesthar/", new ByteArrayResource(submitRequest.getScript().getBytes()));
			
			return new ResponseEntity<SubmitResponse>(new SubmitResponse("status", null, "jobId"), OK);
		} catch(Throwable e) {
			log.error("Job submit error", e);
			
			return new ResponseEntity<SubmitResponse>(new SubmitResponse(null, e.getMessage(), null), INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/api/jobs/{jobId}", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<StatusResponse> status(
			@RequestHeader(value = "PROXY", required = false) String proxy,
			@PathVariable String jobId) {
		return new ResponseEntity<StatusResponse>(new StatusResponse(), OK);
	}
	
	@RequestMapping(value = "/api/jobs/{jobId}/output", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<OutputResponse> output(
			@RequestHeader(value = "PROXY", required = false) String proxy,
			@PathVariable String jobId) {
		return new ResponseEntity<OutputResponse>(new OutputResponse(), OK);
	}
}