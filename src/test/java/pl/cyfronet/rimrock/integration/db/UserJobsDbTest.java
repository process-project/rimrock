package pl.cyfronet.rimrock.integration.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.repositories.JobRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@DirtiesContext
public class UserJobsDbTest {
	@Autowired
	private JobRepository jobRepository;
	
	@Test
	public void testNotTerminalJobQuery() {
		jobRepository.save(new Job("job-1", "RUNNING", "out", "error", "plglogin", "host1", "tag"));
		jobRepository.save(new Job("job-2", "ABORTED", "out", "error", "plglogin", "host1", "tag"));
		jobRepository.save(new Job("job-3", "FINISHED", "out", "error", "plglogin", "host1",
				"tag"));
		jobRepository.save(new Job("job-3", "FINISHED", "out", "error", "plglogin", "host1",
				"tag", "nodes", "cores", "walltime", "queuetime", "start", "end"));
		assertEquals(3, jobRepository.getNotTerminalJobIdsForUserLoginAndHost("plglogin",
				"host1").size());
	}
}