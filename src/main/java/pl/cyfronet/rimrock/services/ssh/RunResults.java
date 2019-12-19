package pl.cyfronet.rimrock.services.ssh;

public class RunResults {
	private String output;
	private String error;
	private int exitCode;
	private boolean timeoutOccured;
	
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public int getExitCode() {
		return exitCode;
	}
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
	@Override
	public String toString() {
		return "RunResults [output=" + output + ", error=" + error + ", exitCode=" + exitCode + ", timeoutOccured=" + timeoutOccured + "]";
	}
	public boolean isTimeoutOccured() {
		return timeoutOccured;
	}
	public void setTimeoutOccured(boolean timeoutOccured) {
		this.timeoutOccured = timeoutOccured;
	}
}