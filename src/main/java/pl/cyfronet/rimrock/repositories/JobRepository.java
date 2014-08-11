package pl.cyfronet.rimrock.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import pl.cyfronet.rimrock.domain.Job;


public interface JobRepository extends CrudRepository<Job, Long> {
	@Query("select distinct job.host from pl.cyfronet.rimrock.domain.Job job") List<String> getHosts();
	List<Job> findByUser(String user);
	Job findOneByJobId(String jobId);
}