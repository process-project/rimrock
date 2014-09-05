package pl.cyfronet.rimrock.controllers.rest.jobs;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import pl.cyfronet.rimrock.controllers.rest.RestHelper;
import pl.cyfronet.rimrock.controllers.rest.RunResponse;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.job.RunException;
import pl.cyfronet.rimrock.services.job.UserJobs;
import pl.cyfronet.rimrock.services.job.UserJobsFactory;

import com.sshtools.j2ssh.util.InvalidStateException;

@Controller
public class JobsController {
	private static final Logger log = LoggerFactory.getLogger(JobsController.class);
	
	@Value("${plgridData.url}")	private String plgDataUrl;
	
	private JobRepository jobRepository;
	private UserJobsFactory userJobsFactory;
	private ProxyHelper proxyHelper;
	
	
	@Autowired
	public JobsController(JobRepository jobRepository, UserJobsFactory userJobsFactory, ProxyHelper proxyHelper) {
		this.jobRepository = jobRepository;
		this.userJobsFactory = userJobsFactory;
		this.proxyHelper = proxyHelper;
	}
	
	@RequestMapping(value = "/api/jobs", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<JobInfo> submit(@RequestHeader("PROXY") String proxy,
			@Valid @RequestBody SubmitRequest submitRequest,
			BindingResult errors) throws CredentialException, GSSException,
			FileManagerException, RunException {
		
		if (errors.hasErrors()) {
			throw new ValidationException(RestHelper.convertErrors(errors));
		}

		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));

		Job job = manager.submit(submitRequest.getHost(),
				submitRequest.getWorkingDirectory(), submitRequest.getScript());

		return new ResponseEntity<JobInfo>(new JobInfo(job, plgDataUrl), CREATED);
	}
	
	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<JobInfo> jobInfo(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) 
			throws JobNotFoundException, CredentialException, GSSException, 
			InvalidStateException, FileManagerException, IOException, InterruptedException {
		log.debug("Processing status request for job with id {}", jobId);
		
		Job job = jobRepository.findOneByJobId(jobId);
		
		if(job == null) {
			throw new JobNotFoundException(jobId);
		}
		
		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));
		manager.update(Arrays.asList(job.getHost()));
		
		job = jobRepository.findOneByJobId(jobId);
		
		return new ResponseEntity<JobInfo>(new JobInfo(job, plgDataUrl), OK);
	}

	@RequestMapping(value = "/api/jobs", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<List<JobInfo>> globalStatus(@RequestHeader("PROXY") String proxy) 
			throws CredentialException, InvalidStateException, GSSException, FileManagerException, 
			IOException, InterruptedException {
		
		List<String> hosts = jobRepository.getHosts();
		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));
		List<Job> jobs = manager.update(hosts);
		List<JobInfo> infos = jobs.stream()
				.map(job -> new JobInfo(job, plgDataUrl))
				.collect(Collectors.toList());
		
		return new ResponseEntity<List<JobInfo>>(infos, OK);
	}

	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = DELETE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> deleteJob(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) 
			throws CredentialException, GSSException, FileManagerException, JobNotFoundException {
		
		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));
		manager.delete(jobId);
		
		return new ResponseEntity<Void>(NO_CONTENT);
	}

	@ExceptionHandler(CredentialException.class)
	private ResponseEntity<RunResponse> handleCredentialsError(CredentialException e) {
		return new ResponseEntity<RunResponse>(new RunResponse(e.getMessage()), FORBIDDEN);
	}
	
	@ExceptionHandler(JobNotFoundException.class)
	private ResponseEntity<RunResponse> handleJobNotFoundError(JobNotFoundException e) {
		return new ResponseEntity<RunResponse>(new RunResponse(e.getMessage()), NOT_FOUND);
	}
	
	@ExceptionHandler(ValidationException.class)
	private ResponseEntity<RunResponse> handleValidationError(CredentialException e) {
		return new ResponseEntity<RunResponse>(new RunResponse(e.getMessage()), UNPROCESSABLE_ENTITY);
	}
	
	@ExceptionHandler({FileManagerException.class, 
		InvalidStateException.class, GSSException.class, 
		IOException.class, InterruptedException.class})
	private ResponseEntity<RunResponse> handleRunCmdError(Exception e) {
		return new ResponseEntity<RunResponse>(new RunResponse(e.getMessage()), INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(RunException.class)
	private ResponseEntity<RunResponse> handleRunError(RunException e) {
		HttpStatus status = e.isTimeoutOccured() ? REQUEST_TIMEOUT : INTERNAL_SERVER_ERROR; 
		return new ResponseEntity<RunResponse>(new RunResponse(e), status);
	}
	
	@ExceptionHandler(Exception.class)
	private ResponseEntity<RunResponse> handleError(Exception e) {
		return new ResponseEntity<RunResponse>(new RunResponse(e.getMessage()), INTERNAL_SERVER_ERROR);
	}
}