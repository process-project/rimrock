package pl.cyfronet.rimrock.services.ssh;


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

		updateErrorExitCode();
	}
	
	public RunException(String message, Throwable cause) {
		super(message, cause);
	}

	private void updateErrorExitCode() {
		if(exitCode == 0) {
			exitCode = -1;
		}
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
