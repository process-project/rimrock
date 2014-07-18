package pl.cyfronet.rimrock.controllers.rest;

import java.io.IOException;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sshtools.j2ssh.util.InvalidStateException;

import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunResults;

@Controller
public class RestController {
	private static final Logger log = LoggerFactory.getLogger(RestController.class);
	
	@Autowired GsisshRunner runner;
	
	@RequestMapping(value = "run", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public RunResponse run(@RequestBody RunRequest runRequest) {
		log.debug("Processing run request {}", runRequest);
		
		RunResults results = null;
		
		try {
			results = runner.run(runRequest.getHost(), runRequest.getProxy(), runRequest.getCommand());
		} catch (CredentialException | InvalidStateException | GSSException
				| IOException | InterruptedException e) {
			log.error("Error", e);
		}
		
		if(results != null) {
			return new RunResponse("0", results.getOutput(), results.getError());
		}
		
		return new RunResponse("1", "error!", "error!");
	}
}