package pl.cyfronet.rimrock.controllers.rest.jobs;

import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.jcraft.jsch.JSchException;

import pl.cyfronet.rimrock.controllers.rest.ErrorResponse;
import pl.cyfronet.rimrock.controllers.rest.RestHelper;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.gsissh.RunException;
import pl.cyfronet.rimrock.services.job.UserJobs;
import pl.cyfronet.rimrock.services.job.UserJobsFactory;

@Controller
public class JobsController {
	private static final Logger log = LoggerFactory.getLogger(JobsController.class);

	private UserJobsFactory userJobsFactory;
	private ProxyHelper proxyHelper;

	@Value("${plgridData.url}")	private String plgDataUrl;

	@Autowired
	public JobsController(UserJobsFactory userJobsFactory, ProxyHelper proxyHelper) {
		this.userJobsFactory = userJobsFactory;
		this.proxyHelper = proxyHelper;
	}

	@RequestMapping(value = "/api/jobs", method = POST, consumes = APPLICATION_JSON_VALUE,
			produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<JobInfo> submit(@RequestHeader("PROXY") String proxy,
			@Valid @RequestBody SubmitRequest submitRequest,
			BindingResult errors) throws CredentialException, GSSException,
			FileManagerException, RunException, KeyStoreException, CertificateException,
			IOException, JSchException {

		if (errors.hasErrors()) {
			throw new ValidationException(errors);
		}

		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));

		Job job = manager.submit(submitRequest.getHost(),
				submitRequest.getWorkingDirectory(), submitRequest.getScript(),
				submitRequest.getTag());

		return new ResponseEntity<>(new JobInfo(job, plgDataUrl), CREATED);
	}

	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<JobInfo> jobInfo(@RequestHeader("PROXY") String proxy,
			@PathVariable("jobId") String jobId)
			throws JobNotFoundException, CredentialException, GSSException,
			FileManagerException, IOException, InterruptedException,
			KeyStoreException, CertificateException, RunException, JSchException {

		log.debug("Processing status request for job with id {}", jobId);

		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));
		Job job = manager.get(jobId);

		if(job == null) {
			throw new JobNotFoundException(jobId);
		}

		if (!Arrays.asList("FINISHED", "ABORTED").contains(job.getStatus())
				|| job.getCores() == null) {
			manager.update(Arrays.asList(job.getHost()), null, asList(jobId));
			job = manager.get(jobId);
		}

		return new ResponseEntity<>(new JobInfo(job, plgDataUrl), OK);
	}

	@RequestMapping(value = "/api/jobs", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<List<JobInfo>> globalStatus(@RequestHeader("PROXY") String proxy,
			@RequestParam(value = "tag", required = false) String tag)
			throws CredentialException, GSSException, FileManagerException,
			IOException, InterruptedException, KeyStoreException, CertificateException,
			RunException, JSchException {
		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));
		List<Job> jobs = manager.update(null, tag, null);
		List<JobInfo> infos = jobs.stream().
				map(job -> new JobInfo(job, plgDataUrl)).
				collect(Collectors.toList());

		return new ResponseEntity<>(infos, OK);
	}

	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = DELETE,
			produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> deleteJob(@RequestHeader("PROXY") String proxy,
			@PathVariable("jobId") String jobId)
			throws CredentialException, GSSException, FileManagerException, JobNotFoundException,
			KeyStoreException, CertificateException, IOException, RunException, JSchException {
		UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));
		manager.delete(jobId);

		return new ResponseEntity<>(NO_CONTENT);
	}

	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = RequestMethod.PUT,
			consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> processJobAction(@RequestHeader("PROXY") String proxy,
			@Valid @RequestBody JobActionRequest actionRequest, BindingResult errors,
			@PathVariable("jobId") String jobId)
			throws CredentialException, GSSException, FileManagerException, JobNotFoundException,
			KeyStoreException, CertificateException, IOException, RunException, JSchException {
		if(errors.hasErrors()) {
			throw new ValidationException(RestHelper.convertErrors(errors));
		}

		if(actionRequest.getAction().equalsIgnoreCase("abort")) {
			UserJobs manager = userJobsFactory.get(proxyHelper.decodeProxy(proxy));
			manager.abort(jobId);
		}

		return new ResponseEntity<>(NO_CONTENT);
	}

	@ExceptionHandler(JobNotFoundException.class)
	private ResponseEntity<ErrorResponse> handleJobNotFoundError(JobNotFoundException e) {
		return new ResponseEntity<>(new ErrorResponse(e.getMessage()), NOT_FOUND);
	}
}
