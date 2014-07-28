package pl.cyfronet.rimrock.controllers.rest.run;

public class RunResponse {
	private String exitCode;
	private String standardOutput;
	private String errorOutput;
	
	public RunResponse(String exitCode, String standardOutput, String errorOutput) {
		this.exitCode = exitCode;
		this.standardOutput = standardOutput;
		this.errorOutput = errorOutput;
	}
	
	public String getExitCode() {
		return exitCode;
	}
	public void setExitCode(String exitCode) {
		this.exitCode = exitCode;
	}
	public String getStandardOutput() {
		return standardOutput;
	}
	public void setStandardOutput(String standardOutput) {
		this.standardOutput = standardOutput;
	}
	public String getErrorOutput() {
		return errorOutput;
	}
	public void setErrorOutput(String errorOutput) {
		this.errorOutput = errorOutput;
	}
}