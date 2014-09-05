package pl.cyfronet.rimrock.controllers.rest.run;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import pl.cyfronet.rimrock.controllers.rest.RestHelper;
import pl.cyfronet.rimrock.controllers.rest.RunResponse;
import pl.cyfronet.rimrock.controllers.rest.RunResponse.Status;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunResults;

@Controller
public class RunController {
	private static final Logger log = LoggerFactory.getLogger(RunController.class);
	
	@Value("${run.timeout.millis}") private int runTimeoutMillis;
	
	private GsisshRunner runner;
	private ProxyHelper proxyHelper;

	@Autowired
	public RunController(GsisshRunner runner, ProxyHelper proxyHelper) {
		this.runner = runner;
		this.proxyHelper = proxyHelper;
	}
	
	@RequestMapping(value = "/api/process", method = GET, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<RunResponse> run(@RequestHeader("PROXY") String proxy, @Valid @RequestBody RunRequest runRequest, BindingResult errors) {
		log.debug("Processing run request {}", runRequest);
		
		if(errors.hasErrors()) {
			return new ResponseEntity<RunResponse>(
					new RunResponse(Status.ERROR, -1, null, null, RestHelper.convertErrors(errors)), UNPROCESSABLE_ENTITY);
		}
		
		try {
			RunResults results = runner.run(runRequest.getHost(), proxyHelper.decodeProxy(proxy), runRequest.getCommand(), -1);
			
			if(results.isTimeoutOccured()) {
				return new ResponseEntity<RunResponse>(
						new RunResponse(Status.ERROR, -1, results.getOutput(), results.getError(),
								"timeout occurred; maximum allowed execution time for this operation is " + runTimeoutMillis + " ms"), REQUEST_TIMEOUT);
			}
			
			return new ResponseEntity<RunResponse>(
					new RunResponse(Status.OK, results.getExitCode(), results.getOutput(), results.getError(), null), OK);
		} catch (Throwable e) {
			log.error("Error", e);
			
			return new ResponseEntity<RunResponse>(new RunResponse(Status.ERROR, -1, null, null, e.getMessage()), INTERNAL_SERVER_ERROR);
		}
	}
}