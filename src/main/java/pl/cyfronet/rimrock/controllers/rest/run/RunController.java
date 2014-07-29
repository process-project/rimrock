package pl.cyfronet.rimrock.controllers.rest.run;

import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import pl.cyfronet.rimrock.controllers.rest.run.RunResponse.Status;
import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunResults;

@Controller
public class RunController {
	private static final Logger log = LoggerFactory.getLogger(RunController.class);
	
	private GsisshRunner runner;

	@Autowired
	public RunController(GsisshRunner runner) {
		this.runner = runner;
	}
	
	@RequestMapping(value = "/api/run", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<RunResponse> run(@Valid @RequestBody RunRequest runRequest, BindingResult errors) {
		log.debug("Processing run request {}", runRequest);
		
		if(errors.hasErrors()) {
			return new ResponseEntity<RunResponse>(
					new RunResponse(Status.error, -1, null, null, convertErrors(errors)),
							HttpStatus.UNPROCESSABLE_ENTITY);
		}
		
		try {
			RunResults results = runner.run(runRequest.getHost(), runRequest.getProxy(), runRequest.getCommand());
			
			return new ResponseEntity<RunResponse>(
					new RunResponse(Status.ok, results.getExitCode(), results.getOutput(), results.getError(), null),
							HttpStatus.OK);
		} catch (Throwable e) {
			log.error("Error", e);
			
			return new ResponseEntity<RunResponse>(
					new RunResponse(Status.error, -1, null, null, e.getMessage()),
							HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String convertErrors(BindingResult errors) {
		return errors.getFieldErrors()
				.stream()
				.map(f -> {
					return f.getField() + ": " + String.join(", ", f.getCodes());
				})
				.collect(Collectors.joining("; "));
	}
}