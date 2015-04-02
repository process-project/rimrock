package pl.cyfronet.rimrock.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import pl.cyfronet.rimrock.domain.GridJob;

public interface GridJobRepository extends CrudRepository<GridJob, Long> {
	List<GridJob> findByUserLogin(String userLogin);
	GridJob findOneByJobId(String jobId);
	GridJob findOneByJobIdAndUserLogin(String jobId, String userLogin);
	List<GridJob> findByUserLoginAndTag(String userLogin, String tag);
}