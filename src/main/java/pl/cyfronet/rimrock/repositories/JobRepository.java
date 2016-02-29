package pl.cyfronet.rimrock.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import pl.cyfronet.rimrock.domain.Job;

public interface JobRepository extends CrudRepository<Job, Long> {
	@Query("SELECT DISTINCT job.host FROM Job job WHERE userLogin = :userLogin")
	List<String> getHosts(@Param("userLogin") String userLogin);
	
	List<Job> findByUserLogin(String userLogin);
	
	Job findOneByJobId(String jobId);
	
	@Query("SELECT job FROM Job job WHERE userLogin = :userLogin AND host in :hosts")
	List<Job> findByUsernameOnHosts(@Param("userLogin") String userLogin,
			@Param("hosts") List<String> hosts);
	
	Job findOneByJobIdAndUserLogin(String jobId, String userLogin);

	@Query("SELECT job FROM Job job WHERE userLogin = :userLogin AND tag = :tag AND host in :hosts")
	List<Job> findByUsernameAndTagOnHosts(@Param("userLogin") String userLogin,
			@Param("tag") String tag, @Param("hosts") List<String> hosts);

	@Query("SELECT job.jobId FROM Job job WHERE userLogin = :userLogin AND host = :host")
	List<String> getJobIdsForUserLoginAndHost(@Param("userLogin") String userLogin, @Param("host") String host);
}