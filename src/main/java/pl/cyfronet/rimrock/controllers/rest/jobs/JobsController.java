package pl.cyfronet.rimrock.controllers.rest.jobs;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.Valid;

import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import pl.cyfronet.rimrock.controllers.rest.RestHelper;
import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunResults;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sshtools.j2ssh.util.InvalidStateException;

@Controller
public class JobsController {
	private static final Logger log = LoggerFactory.getLogger(JobsController.class);
	
	private FileManagerFactory fileManagerFactory;
	private GsisshRunner runner;
	private ObjectMapper mapper;

	@Autowired
	public JobsController(FileManagerFactory fileManagerFactory, GsisshRunner runner, ObjectMapper mapper) {
		this.fileManagerFactory = fileManagerFactory;
		this.runner = runner;
		this.mapper = mapper;
	}
	
	@RequestMapping(value = "/api/jobs", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<SubmitResponse> submit(@RequestHeader("PROXY") String proxy, @Valid @RequestBody SubmitRequest submitRequest, BindingResult errors) {
		log.debug("Processing run request {}", submitRequest);
		
		if(errors.hasErrors()) {
			return new ResponseEntity<SubmitResponse>(new SubmitResponse(null, RestHelper.convertErrors(errors), null), UNPROCESSABLE_ENTITY);
		}
		
		try {
			FileManager fileManager = fileManagerFactory.get(RestHelper.decodeProxy(proxy));
			String rootPath = buildRootPath(submitRequest.getHost(), submitRequest.getWorkingDirectory(), RestHelper.decodeProxy(proxy));
			fileManager.cp(rootPath + "script.sh", new ByteArrayResource(submitRequest.getScript().getBytes()));
			fileManager.cp(rootPath + "start", new ClassPathResource("scripts/start"));
			
			RunResults result = runner.run(submitRequest.getHost(), RestHelper.decodeProxy(proxy), "cd " + rootPath + "; chmod +x start; ./start script.sh", 60000);
			
			if(result.isTimeoutOccured() || result.getExitCode() != 0) {
				return new ResponseEntity<SubmitResponse>(new SubmitResponse("ERROR", result.getError(), null), INTERNAL_SERVER_ERROR);
			} else {
				SubmitResult submitResult = mapper.readValue(result.getOutput(), SubmitResult.class);
				
				if("OK".equals(submitResult.getResult())) {
					return new ResponseEntity<SubmitResponse>(new SubmitResponse(submitResult.getResult(), null, submitResult.getJobId()), OK);
				} else {
					return new ResponseEntity<SubmitResponse>(new SubmitResponse(submitResult.getResult(), submitResult.getErrorMessage(), null), INTERNAL_SERVER_ERROR);
				}
			}
		} catch(Throwable e) {
			log.error("Job submit error", e);
			
			return new ResponseEntity<SubmitResponse>(new SubmitResponse("ERROR", e.getMessage(), null), INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<StatusResponse> status(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) {
		log.debug("Processing status request for job with id {}", jobId);
		
		try {
			StatusResult statusResult = getStatusResult(RestHelper.decodeProxy(proxy));
			
			String statusValue = findStatusValue(jobId, statusResult.getStatuses());
			
			if(statusValue == null) {
				return new ResponseEntity<StatusResponse>(new StatusResponse(jobId, "ERROR", "A job with the given job id could not be found"), NOT_FOUND);
			} else {
				return new ResponseEntity<StatusResponse>(new StatusResponse(jobId, statusValue, null), OK);
			}
		} catch (CredentialException | InvalidStateException | GSSException | IOException | InterruptedException | FileManagerException e) {
			log.error("Job status retrieval error", e);
			
			return new ResponseEntity<StatusResponse>(new StatusResponse(jobId, "ERROR", e.getMessage()), INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/api/jobs", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<StatusResponse> globalStatus(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) {
		return new ResponseEntity<StatusResponse>(new StatusResponse(null, null, null), OK);
	}
	
	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = DELETE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<StatusResponse> deleteJob(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) {
		return new ResponseEntity<StatusResponse>(new StatusResponse(null, null, null), OK);
	}
	
	@RequestMapping(value = "/api/jobs/{jobId}/output", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<OutputResponse> output(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) {
		return new ResponseEntity<OutputResponse>(new OutputResponse(), OK);
	}
	
	private String findStatusValue(String jobId, List<Status> statuses) {
		if(statuses != null) {
			for(Status status : statuses) {
				if(status.getJobId() != null && status.getJobId().equals(jobId)) {
					return status.getStatus();
				}
			}
		}
		
		return null;
	}
	
	private StatusResult getStatusResult(String proxy) throws CredentialException, InvalidStateException, GSSException, IOException, InterruptedException, FileManagerException {
		//TODO(DH): obtain host from db
		String host = "zeus.cyfronet.pl";
		
		FileManager fileManager = fileManagerFactory.get(proxy);
		String rootPath = getRootPath(host, proxy);
		fileManager.cp(rootPath + ".rimrock/status", new ClassPathResource("scripts/status"));
		RunResults result = runner.run(host, proxy, "cd " + rootPath + ".rimrock; chmod +x status; ./status", -1);
		
		if(result.isTimeoutOccured() || result.getExitCode() != 0) {
			StatusResult statusResult = new StatusResult();
			statusResult.setResult("ERROR");
			statusResult.setErrorMessage(result.getError());
			
			return statusResult;
		} else {
			return mapper.readValue(result.getOutput(), StatusResult.class);
		}
	}
	
	private String buildRootPath(String host, String workingDirectory, String proxy) throws CredentialException, GSSException {
		String rootPath = workingDirectory == null ? getRootPath(host, proxy) : workingDirectory;
		
		return rootPath;
	}

	private String getRootPath(String host, String proxy) throws CredentialException, GSSException {
		switch(host.trim()) {
			case "zeus.cyfronet.pl":
			case "ui.cyfronet.pl":
				return "/people/" + getUserLogin(proxy) + "/";
			default:
				throw new IllegalArgumentException("Without submitting a working directory only zeus.cyfronet.pl host is supported");
		}
	}

	private String getUserLogin(String proxyValue) throws CredentialException, GSSException {
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(proxyValue.getBytes()));
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		String dn = gsscredential.getName().toString();
		Pattern pattern = Pattern.compile(".*=(.*)$");
		Matcher matcher = pattern.matcher(dn);
		
		if(matcher.matches()) {
			return matcher.group(1);
		} else {
			throw new IllegalArgumentException("Could not extract user name from the supplied user proxy");
		}
	}
}