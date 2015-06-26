package pl.cyfronet.rimrock.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import pl.cyfronet.rimrock.domain.GridJob;
import pl.cyfronet.rimrock.domain.GridJob.Middleware;

public interface GridJobRepository extends CrudRepository<GridJob, Long> {
	GridJob findOneByJobIdAndUserLogin(String jobId, String userLogin);
	List<GridJob> findByUserLoginAndMiddleware(String userLogin, Middleware middleware);
	List<GridJob> findByUserLoginAndTagAndMiddleware(String userLogin, String tag, Middleware middleware);
}