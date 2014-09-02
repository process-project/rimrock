package pl.cyfronet.rimrock.controllers.rest.jobs;

public class JobNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public JobNotFoundException(String jobId) {
		super(String.format("Job with %s id does not exist", jobId));
	}
}
