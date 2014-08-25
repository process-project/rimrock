package pl.cyfronet.rimrock.services.job;

import pl.cyfronet.rimrock.services.RunResults;

public class RunException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String output;
	private String error;
	private int exitCode;
	private boolean timeoutOccured;

	public RunException(String message) {
		super(message);
	}
	
	public RunException(String message, RunResults run) {
		super(message);
		output = run.getOutput();
		error = run.getError();
		exitCode = run.getExitCode();
		timeoutOccured = run.isTimeoutOccured();
	}
	
	public String getOutput() {
		return output;
	}

	public String getError() {
		return error;
	}

	public int getExitCode() {
		return exitCode;
	}

	public boolean isTimeoutOccured() {
		return timeoutOccured;
	}
}
