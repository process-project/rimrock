package pl.cyfronet.rimrock.controllers.rest.run;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;

import javax.validation.Valid;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
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

import com.jcraft.jsch.JSchException;

import pl.cyfronet.rimrock.controllers.rest.RunResponse;
import pl.cyfronet.rimrock.controllers.rest.RunResponse.Status;
import pl.cyfronet.rimrock.controllers.rest.jobs.ValidationException;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.services.gsissh.GsisshRunner;
import pl.cyfronet.rimrock.services.gsissh.RunException;
import pl.cyfronet.rimrock.services.gsissh.RunResults;

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

	@RequestMapping(value = "/api/process", method = POST, consumes = APPLICATION_JSON_VALUE,
			produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<RunResponse> run(@RequestHeader("PROXY") String proxy,
			@Valid @RequestBody RunRequest runRequest, BindingResult errors)
					throws CredentialException, GSSException, IOException, InterruptedException,
					KeyStoreException, CertificateException, JSchException {
		log.debug("Processing run request {}", runRequest);

		if(errors.hasErrors()) {
			throw new ValidationException(errors);
		}

		RunResults results = runner.run(runRequest.getHost(), proxyHelper.decodeProxy(proxy),
				runRequest.getCommand(), runRequest.getWorkingDirectory(), -1);

		if(results.isTimeoutOccured()) {
			throw new RunException(
					"timeout occurred; maximum allowed execution time for this operation is "
			+ runTimeoutMillis + " ms", results);
		}

		return new ResponseEntity<>(
				new RunResponse(Status.OK, results.getExitCode(), results.getOutput(),
						results.getError(), null), OK);
	}
}
