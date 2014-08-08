package pl.cyfronet.rimrock.integration.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.repositories.JobRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class DbBasicTest {
	@Autowired private JobRepository jobRepository;
	
	@Test
	public void testJobCrud() {
		Job job = new Job("jobId", "putput", "error", "user", "host");
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
}