package pl.cyfronet.rimrock.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import pl.cyfronet.rimrock.controllers.rest.jobs.JobsController;
import pl.cyfronet.rimrock.controllers.rest.jobs.Status;
import pl.cyfronet.rimrock.controllers.rest.jobs.StatusResponse;
import pl.cyfronet.rimrock.controllers.rest.jobs.StatusResult;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;
import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunResults;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sshtools.j2ssh.util.InvalidStateException;

public class JobMergingTest {
	@Mock FileManagerFactory fileManagerFactory;
	@Mock GsisshRunner runner;
	@Mock ObjectMapper mapper;
	@Mock JobRepository jobRepository;
	@Mock ProxyHelper proxyHelper;
	@Mock FileManager fileManager;
	
	private JobsController jobsController;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		jobsController = new JobsController(fileManagerFactory, runner, mapper, jobRepository, proxyHelper);
	}
	
	@Test
	public void testSingleJobMerge() throws CredentialException, GSSException, InvalidStateException, IOException, InterruptedException {
		String host = "zeus.cyfronet.pl";
		String expected = "RUNNING";
		String jobId = "firstJobId";
		
		when(fileManagerFactory.get("proxy")).thenReturn(fileManager);
		when(proxyHelper.getUserLogin("proxy")).thenReturn("plguser");
		when(jobRepository.findOneByJobId(Mockito.eq(jobId))).thenReturn(new Job(jobId, "out", "error", "plguser", host));
		when(runner.run(Mockito.eq(host), Mockito.eq("proxy"), Mockito.anyString(), Mockito.anyInt())).thenReturn(new RunResults());
		
		StatusResult result = new StatusResult();
		result.setResult("OK");
		
		List<Status> statuses = new ArrayList<Status>();
		result.setStatuses(statuses);
		Status status = new Status();
		status.setJobId(jobId);
		status.setStatus(expected);
		statuses.add(status);
		when(mapper.readValue(Mockito.anyString(), (Class<StatusResult>) Mockito.any(Class.class))).thenReturn(result);
		
		//Base64.decode("cHJveHk=") == "proxy"
		ResponseEntity<StatusResponse> response = jobsController.status("cHJveHk=", jobId);
		
		assertEquals(jobId, response.getBody().getJobId());
		assertEquals(expected, response.getBody().getStatus());
	}
	
	@Test
	public void testJobMergeWithNoJobsFromHost() throws CredentialException, GSSException, InvalidStateException, IOException, InterruptedException {
		String host = "zeus.cyfronet.pl";
		String expected = "FINISHED";
		String jobId = "firstJobId";
		String user = "plguser";
		
		when(fileManagerFactory.get("proxy")).thenReturn(fileManager);
		when(proxyHelper.getUserLogin("proxy")).thenReturn(user);
		when(runner.run(Mockito.eq(host), Mockito.eq("proxy"), Mockito.anyString(), Mockito.anyInt())).thenReturn(new RunResults());
		when(jobRepository.findOneByJobId(Mockito.eq(jobId))).thenReturn(new Job(jobId, "out", "error", user, host));
		
		List<Job> jobs = new ArrayList<Job>();
		jobs.add(new Job(jobId, "output", "error", user, host));
		when(jobRepository.findByUser(user)).thenReturn(jobs);
		
		StatusResult result = new StatusResult();
		result.setResult("OK");
		
		result.setStatuses(new ArrayList<Status>());
		when(mapper.readValue(Mockito.anyString(), (Class<StatusResult>) Mockito.any(Class.class))).thenReturn(result);
		
		//Base64.decode("cHJveHk=") == "proxy"
		ResponseEntity<StatusResponse> response = jobsController.status("cHJveHk=", jobId);
		
		assertNull(response.getBody().getErrorMessage());
		assertEquals(jobId, response.getBody().getJobId());
		assertEquals(expected, response.getBody().getStatus());
	}
}