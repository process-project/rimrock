package pl.cyfronet.rimrock.repositories;

import org.springframework.data.repository.CrudRepository;

import pl.cyfronet.rimrock.domain.InteractiveProcess;

public interface InteractiveProcessRepository extends CrudRepository<InteractiveProcess, Long>{
	InteractiveProcess findByProcessId(String processId);
}