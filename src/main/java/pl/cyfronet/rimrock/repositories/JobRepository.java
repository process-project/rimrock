package pl.cyfronet.rimrock.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import pl.cyfronet.rimrock.domain.Job;


public interface JobRepository extends CrudRepository<Job, Long> {
	@Query("select distinct job.host from Job job") 
	List<String> getHosts();
	
	List<Job> findByUser(String user);
	
	Job findOneByJobId(String jobId);	
	
	@Query("SELECT job FROM Job job WHERE user = :user AND host in :hosts")
	List<Job> findByUserOnHosts(@Param("user") String user, @Param("hosts") List<String> hosts);
}
