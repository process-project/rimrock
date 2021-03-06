package pl.cyfronet.rimrock.services.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.ietf.jgss.GSSException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;
import pl.cyfronet.rimrock.services.gsissh.GsisshRunner;
import pl.cyfronet.rimrock.services.gsissh.RunException;
import pl.cyfronet.rimrock.services.gsissh.RunResults;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RimrockApplication.class)
@DirtiesContext
public class UserJobsTest {
	@Mock private FileManagerFactory fileManagerFactory;
	@Mock private FileManager fileManager;
	@Mock private GsisshRunner runner;
	@Mock private ProxyHelper proxyHelper;

	@Autowired private ObjectMapper mapper;
	@Autowired private JobRepository jobRepository;

	private UserJobs userJobs;
	private String proxy = "proxy";
	private String userLogin = "userLogin";

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		jobRepository.deleteAll();
		when(fileManagerFactory.get(proxy)).thenReturn(fileManager);
		when(proxyHelper.getUserLogin(proxy)).thenReturn(userLogin);
		userJobs = new UserJobs(proxy, fileManagerFactory, runner,
				jobRepository, proxyHelper, mapper);
	}

	@Test
	public void testSuccessJobSubmit() throws Exception {
		RunResults result = new RunResults();
		result.setExitCode(0);
		result.setTimeoutOccured(false);
		result.setOutput("{\"result\": \"OK\", \"job_id\": \"jobId\", \"standard_output\": \"stdout\", \"standard_error\": \"stderr\"}");

		when(
			runner.run(eq("host"), eq(proxy),
					startsWith("cd /home/dir/; chmod +x .rimrock/start; ./.rimrock/start script-"),
					any(), anyInt())).thenReturn(result);

		Job job = userJobs.submit("host", "/home/dir", "script payload", null);

		verify(fileManager).cp(startsWith("/home/dir/script-"), any(Resource.class));
		verify(fileManager).cp(eq("/home/dir/.rimrock/start"), any(Resource.class));

		assertEquals("jobId", job.getJobId());
		assertEquals("QUEUED", job.getStatus());
		assertEquals(userLogin, job.getUserLogin());
		assertEquals("host", job.getHost());
		assertEquals("stdout", job.getStandardOutputLocation());
		assertEquals("stderr", job.getStandardErrorLocation());
	}

	@Test
	public void testCpFailureWhenSubmittingJob() throws Exception {
		doThrow(FileManagerException.class).when(fileManager).cp(anyString(),
				any(Resource.class));

		try {
			userJobs.submit("host", "/home/dir", "script payload", null);
			fail();
		} catch (FileManagerException e) {
			// ok should be thrown
		}
	}

	@Test
	public void testRunFailureWhenSubmittingJob() throws Exception {
		when(
			runner.run(eq("host"), eq(proxy),
					startsWith("cd /home/dir/; chmod +x .rimrock/start; ./.rimrock/start script-"),
					any(), anyInt())).thenThrow(new GSSException(1));

		try {
			userJobs.submit("host", "/home/dir", "script payload", null);
			fail();
		} catch (RunException e) {
			// ok should be thrown
		}
	}

	@Test
	public void testRunResponseCoruptedWhenSubmittingJob() throws Exception {
		RunResults result = new RunResults();
		result.setExitCode(0);
		result.setTimeoutOccured(false);
		result.setOutput("{\"corrupted\": true}");

		when(
			runner.run(eq("host"), eq(proxy),
					startsWith("cd /home/dir/; chmod +x .rimrock/start; ./.rimrock/start script-"),
					any(), anyInt())).thenReturn(result);

		try {
			userJobs.submit("host", "/home/dir", "script payload", null);
			fail();
		} catch (RunException e) {
			// ok should be thrown
		}
	}

	@Test
	public void testUpdateJobStatus() throws Exception {
		createJob("1", userLogin, "zeus.cyfronet.pl");
		createJob("2", userLogin, "zeus.cyfronet.pl");
		createJob("3", userLogin, "ui.cyfronet.pl");
		createJob("4", userLogin, "host3");
		createJob("5", "another_user", "zeus.cyfronet.pl");

		RunResults zeusResult = new RunResults();
		zeusResult
				.setOutput("{\"statuses\": [{\"job_id\": \"1\", \"job_state\": \"RUNNING\"}]," +
						"\"history\": []," +
						"\"result\": \"OK\"}");

		RunResults uiResult = new RunResults();
		uiResult.setOutput("{\"statuses\": [{\"job_id\": \"3\", \"job_state\": \"ERROR\"}]," +
				"\"history\": []," +
				"\"result\": \"OK\"}");

		when(
				runner.run(
						eq("zeus.cyfronet.pl"),
						eq(proxy),
						startsWith("cd /people/userLogin/.rimrock; chmod +x status; ./status"),
						any(), anyInt())).thenReturn(zeusResult);
		when(
				runner.run(
						eq("ui.cyfronet.pl"),
						eq(proxy),
						startsWith("cd /people/userLogin/.rimrock; chmod +x status; ./status"),
						any(), anyInt())).thenReturn(uiResult);

		userJobs.update(Arrays.asList("zeus.cyfronet.pl", "ui.cyfronet.pl"), null, null);

		assertEquals("RUNNING", jobStatus("1"));
		assertEquals("FINISHED", jobStatus("2"));
		assertEquals("ERROR", jobStatus("3"));
		assertEquals("QUEUED", jobStatus("4"));
		assertEquals("QUEUED", jobStatus("5"));
	}

	@Test
	public void testUpdateJobHistory() throws Exception {
		String jobId = "1";
		createJob(jobId, userLogin, "zeus.cyfronet.pl", "FINISHED");

		RunResults zeusResult = new RunResults();
		zeusResult
				.setOutput("{\"statuses\": [], " +
						"\"history\": [{" +
						"\"job_id\": \"1\"," +
						"\"job_nodes\": \"1\"," +
						"\"job_cores\": \"12\"," +
						"\"job_walltime\": \"00:00:04\"," +
						"\"job_queuetime\": \"00:00:25\"," +
						"\"job_starttime\": \"2015-09-03 10:26:37\"," +
						"\"job_endtime\": \"2015-09-03 10:26:40\"" +
						"}], " +
						"\"result\": \"OK\"}");

		when(
				runner.run(
						eq("zeus.cyfronet.pl"),
						eq(proxy),
						startsWith("cd /people/userLogin/.rimrock; chmod +x status; ./status"),
						any(), anyInt())).thenReturn(zeusResult);

		userJobs.update(Arrays.asList("zeus.cyfronet.pl"), null, null);

		Job job = jobRepository.findOneByJobId(jobId);

		assertEquals("FINISHED", job.getStatus());
		assertEquals("1", job.getNodes());
		assertEquals("12", job.getCores());
		assertEquals("00:00:04", job.getWallTime());
		assertEquals("00:00:25", job.getQueueTime());
		assertEquals("2015-09-03 10:26:37", job.getStartTime());
		assertEquals("2015-09-03 10:26:40", job.getEndTime());
	}


	@Test
	public void testUpdateJobStatusesWhenNoHosts() throws Exception {
		List<Job> jobs = userJobs.update(Arrays.asList(), null, null);

		assertEquals(0, jobs.size());
	}

	@Test
	public void testDeleteRunningJob() throws Exception {
		createJob("to_delete", userLogin, "zeus.cyfronet.pl");
		when(
				runner.run(
						eq("zeus.cyfronet.pl"),
						eq(proxy),
						eq("cd /people/userLogin/.rimrock; chmod +x stop; ./stop to_delete"),
						any(), anyInt())).thenReturn(new RunResults());

		userJobs.delete("to_delete");

		assertNull(jobRepository.findOneByJobId("to_delete"));
	}

	@Test
	public void testDeleteFinishedJob() throws Exception {
		Job job = new Job("finished_to_delete", "FINISHED", "", "", userLogin, "zeus.cyfronet.pl",
				null);
		jobRepository.save(job);

		userJobs.delete("finished_to_delete");

		verify(runner, times(0))
				.run(eq("zeus.cyfronet.pl"),
						eq(proxy),
						eq("cd /people/userLogin/.rimrock; chmod +x stop; ./stop finished_to_delete"),
						any(), anyInt());
		assertNull(jobRepository.findOneByJobId("to_delete"));
	}

	@Test
	public void testGetUserJob() throws Exception {
		Job userJob = createJob("user_job", userLogin, "zeus.cyfronet.pl");

		Job job = userJobs.get(userJob.getJobId());

		assertEquals(userJob.getId(), job.getId());
	}

	public void testNotGetOtherUserJob() throws Exception {
		Job userJob = createJob("different_user_job", "different_user", "zeus.cyfronet.pl");

		Job job = userJobs.get(userJob.getJobId());

		assertNull(job);
	}

	private Job createJob(String id, String username, String hostname) {
		return createJob(id, username, hostname, "QUEUED");
	}

	private Job createJob(String id, String username, String hostname, String status) {
		Job job = new Job(id, status, "", "", username, hostname, null);
		jobRepository.save(job);

		return job;
	}

	private String jobStatus(String jobId) {
		return jobRepository.findOneByJobId(jobId).getStatus();
	}
}
