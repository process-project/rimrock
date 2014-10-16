package pl.cyfronet.rimrock.controllers.rest.irun;

public class InteractiveProcessNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 5713869095426546169L;

	public InteractiveProcessNotFoundException(String processId) {
		super(String.format("Interactive process with id %s cannot be  found", processId));
	}
}
