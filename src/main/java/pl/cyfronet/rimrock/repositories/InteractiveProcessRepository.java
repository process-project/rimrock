package pl.cyfronet.rimrock.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import pl.cyfronet.rimrock.domain.InteractiveProcess;

public interface InteractiveProcessRepository extends CrudRepository<InteractiveProcess, Long> {
	InteractiveProcess findBySecret(String secret);
	
	InteractiveProcess findByProcessId(String processId);	
	
	List<InteractiveProcess> findByUserLogin(String userLogin);
	
	List<InteractiveProcess> findByUserLoginAndTag(String userLogin, String tag);
}