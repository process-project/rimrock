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
import pl.cyfronet.rimrock.services.ssh.GsisshRunner;
import pl.cyfronet.rimrock.services.ssh.RunException;
import pl.cyfronet.rimrock.services.ssh.RunResults;
import pl.cyfronet.rimrock.services.ssh.SshRunner;

@Controller
public class RunController {
	private static final Logger log = LoggerFactory.getLogger(RunController.class);

	@Value("${run.timeout.millis}") private int runTimeoutMillis;

	private GsisshRunner gsiRunner;
	private SshRunner sshRunner;
	private ProxyHelper proxyHelper;

	@Autowired
	public RunController(GsisshRunner gsiRunner, ProxyHelper proxyHelper, SshRunner sshRunner) {
		this.gsiRunner = gsiRunner;
		this.sshRunner = sshRunner;
		this.proxyHelper = proxyHelper;
	}

	@RequestMapping(value = "/api/process", method = POST, consumes = APPLICATION_JSON_VALUE,
			produces = APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<RunResponse> run(@RequestHeader("PROXY") String proxy,
			@RequestHeader(name="SSH_USER", required=false) String sshUser,
			@RequestHeader(name="SSH_PRIV_KEY", required=false) String sshPrivKey,
			@Valid @RequestBody RunRequest runRequest, BindingResult errors)
					throws CredentialException, GSSException, IOException, InterruptedException,
					KeyStoreException, CertificateException, JSchException {
		log.debug("Processing run request {}", runRequest);

		if(errors.hasErrors()) {
			throw new ValidationException(errors);
		}

		RunResults results; 
		
		if(sshUser == null || sshPrivKey == null) {
			results = gsiRunner.run(runRequest.getHost(), proxyHelper.decodeProxy(proxy),
				runRequest.getCommand(), runRequest.getWorkingDirectory(), -1);
		} else {
			results = sshRunner.run(runRequest.getHost(), proxyHelper.decodeProxy(proxy),
				runRequest.getCommand(), runRequest.getWorkingDirectory(), -1,
				sshUser, sshPrivKey);
		}

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
