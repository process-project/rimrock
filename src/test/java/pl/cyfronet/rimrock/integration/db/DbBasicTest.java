package pl.cyfronet.rimrock.integration.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
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
public class DbBasicTest {
	@Autowired private JobRepository jobRepository;
	
	@Before
	public void setUp() {
		jobRepository.deleteAll();
	}
	
	@After
	public void tearDown() {
		jobRepository.deleteAll();
	}
	
	@Test
	public void testJobCrud() {
		Job job = new Job("jobId", "ACTIVE", "putput", "error", "user", "host", "tag");
		jobRepository.save(job);
		assertNotNull(job.getId());
		assertTrue(job.getId() > 0);
		
		long id = job.getId();
		Job found = jobRepository.findOne(id);
		assertNotNull(found);
		assertEquals("jobId", found.getJobId());
		
		found.setJobId("anotherJobId");
		jobRepository.save(found);
		found = jobRepository.findOne(id);
		assertEquals("anotherJobId", found.getJobId());
		
		jobRepository.delete(id);
		assertEquals(0, jobRepository.count());
	}
	
	@Test
	public void testFindByUserAndHosts() {
		Job j1 = userJob("1", "user1", "host1");
		Job j2 = userJob("2", "user1", "host2");
		jobRepository.save(j1);
		jobRepository.save(j2);
		jobRepository.save(userJob("3", "user1", "host3"));
		jobRepository.save(userJob("4", "user2", "host1"));
		
		List<Job> jobs = jobRepository.findByUsernameOnHosts("user1", Arrays.asList("host1", "host2"));
		
		assertEquals(2, jobs.size());		
		assertEquals("1", jobs.get(0).getJobId());
		assertEquals("2", jobs.get(1).getJobId());
	}
	
	private Job userJob(String id, String username, String hostname) {
		return new Job(id, "ACTIVE", "", "", username, hostname, null);
	}
}