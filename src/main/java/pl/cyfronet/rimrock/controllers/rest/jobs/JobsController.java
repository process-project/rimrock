package pl.cyfronet.rimrock.controllers.rest.jobs;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.globus.gsi.CredentialException;
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
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;
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
	private JobRepository jobRepository;
	private ProxyHelper proxyHelper;

	@Autowired
	public JobsController(FileManagerFactory fileManagerFactory, GsisshRunner runner, ObjectMapper mapper,
			JobRepository jobRepository, ProxyHelper proxyHelper) {
		this.fileManagerFactory = fileManagerFactory;
		this.runner = runner;
		this.mapper = mapper;
		this.jobRepository = jobRepository;
		this.proxyHelper = proxyHelper;
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
					jobRepository.save(new Job(submitResult.getJobId(), submitResult.getStandardOutputLocation(), submitResult.getStandardErrorLocation(), proxyHelper.getUserLogin(proxy), submitRequest.getHost()));
					
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
			Job job = jobRepository.findOneByJobId(jobId);
			
			if(job == null) {
				return new ResponseEntity<StatusResponse>(new StatusResponse(null, "ERROR", "Job with id " + jobId + " does not exist"), NOT_FOUND);
			}
			
			StatusResult statusResult = getStatusResult(RestHelper.decodeProxy(proxy), job.getHost());
			String statusValue = findStatusValue(jobId, statusResult.getStatuses(), proxyHelper.getUserLogin(RestHelper.decodeProxy(proxy)));
			
			if(statusValue == null) {
				return new ResponseEntity<StatusResponse>(new StatusResponse(jobId, "ERROR", "A job with the given job id could not be found"), NOT_FOUND);
			} else {
				return new ResponseEntity<StatusResponse>(new StatusResponse(jobId, statusValue, null), OK);
			}
		} catch (Throwable e) {
			log.error("Job status for job with id " + jobId + " retrieval error", e);
			
			return new ResponseEntity<StatusResponse>(new StatusResponse(jobId, "ERROR", e.getMessage()), INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/api/jobs", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<GlobalStatusResponse> globalStatus(@RequestHeader("PROXY") String proxy) {
		try {
			List<String> hosts = jobRepository.getHosts();
			List<Status> statuses = new ArrayList<Status>();
			
			for(String host: hosts) {
				StatusResult statusResult = getStatusResult(RestHelper.decodeProxy(proxy), host);
				
				if(statusResult.getErrorMessage() != null) {
					return new ResponseEntity<GlobalStatusResponse>(new GlobalStatusResponse("ERROR", statusResult.getErrorMessage(), null), INTERNAL_SERVER_ERROR);
				}
				
				statuses.addAll(statusResult.getStatuses());
			}

			String userLogin = proxyHelper.getUserLogin(RestHelper.decodeProxy(proxy));
			return new ResponseEntity<GlobalStatusResponse>(new GlobalStatusResponse("OK", null, mapStatuses(mergeStatuses(statuses, userLogin), userLogin)), OK);
		} catch (Throwable e) {
			log.error("Job status retrieval error", e);
			
			return new ResponseEntity<GlobalStatusResponse>(new GlobalStatusResponse("ERROR", e.getMessage(), null), INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/api/jobs/{jobId:.+}", method = DELETE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<StopResponse> deleteJob(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) throws CredentialException, GSSException, FileManagerException {
		Job job = jobRepository.findOneByJobId(jobId);
		
		if(job == null) {
			return new ResponseEntity<StopResponse>(new StopResponse("ERROR", "Job with id " + jobId + " does not exist"), NOT_FOUND);
		}
		
		String host = job.getHost();
		FileManager fileManager = fileManagerFactory.get(RestHelper.decodeProxy(proxy));
		String rootPath = getRootPath(host, RestHelper.decodeProxy(proxy));
		fileManager.cp(rootPath + ".rimrock/stop", new ClassPathResource("scripts/stop"));
		
		try {
			RunResults result = runner.run(host, RestHelper.decodeProxy(proxy), "cd " + rootPath + ".rimrock; chmod +x stop; ./stop " + jobId, -1);
			
			if(result.isTimeoutOccured() || result.getExitCode() != 0) {
				return new ResponseEntity<StopResponse>(new StopResponse("ERROR", result.getError()), INTERNAL_SERVER_ERROR);
			} else {
				StopResult stopResult = mapper.readValue(result.getOutput(), StopResult.class);
				
				return new ResponseEntity<StopResponse>(new StopResponse(stopResult.getResult(), stopResult.getErrorMessage()), OK);
			}
		} catch (Throwable e) {
			log.error("Stopping job with id " + jobId + " failed", e);
			
			return new ResponseEntity<StopResponse>(new StopResponse("ERROR", e.getMessage()), INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/api/jobs/{jobId}/output", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<OutputResponse> output(@RequestHeader("PROXY") String proxy, @PathVariable String jobId) {
		return new ResponseEntity<OutputResponse>(new OutputResponse(), OK);
	}
	
	private List<StatusResponse> mapStatuses(List<Status> statuses, String user) {
		List<StatusResponse> result = new ArrayList<>();
		List<Status> mergedStatuses = mergeStatuses(statuses, user);

		for(Status status : mergedStatuses) {
			result.add(new StatusResponse(status.getJobId(), status.getStatus(), status.getErrorMessage()));
		}
		
		return result;
	}

	private String findStatusValue(String jobId, List<Status> statuses, String user) {
		if(statuses != null) {
			List<Status> mergedStatuses = mergeStatuses(statuses, user);
			
			for(Status status : mergedStatuses) {
				if(status.getJobId() != null && status.getJobId().equals(jobId)) {
					return status.getStatus();
				}
			}
		}
		
		return null;
	}
	
	private List<Status> mergeStatuses(List<Status> statuses, String user) {
		List<Job> dbJobs = jobRepository.findByUser(user);
		Map<String, Status> mappedStatusJobIds = statuses
				.stream()
				.collect(Collectors.toMap(Status::getJobId, Function.<Status>identity()));
		List<Status> result = new ArrayList<Status>();
		
		for(Job dbJob : dbJobs) {
			if(!mappedStatusJobIds.keySet().contains(dbJob.getJobId())) {
				Status status = new Status();
				status.setJobId(dbJob.getJobId());
				status.setStatus("FINISHED");
				result.add(status);
			} else {
				result.add(mappedStatusJobIds.get(dbJob.getJobId()));
			}
		}
		
		return result;
	}
	
	private StatusResult getStatusResult(String proxy, String host) throws CredentialException, InvalidStateException, GSSException, IOException, InterruptedException, FileManagerException {
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
				return "/people/" + proxyHelper.getUserLogin(proxy) + "/";
			default:
				throw new IllegalArgumentException("Without submitting a working directory only zeus.cyfronet.pl host is supported");
		}
	}
}