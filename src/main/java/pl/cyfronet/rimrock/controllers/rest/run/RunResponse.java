package pl.cyfronet.rimrock.controllers.rest.run;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RunResponse {
	@JsonProperty("exit_code")
	private String exitCode;
	
	@JsonProperty("standard_output")
	private String standardOutput;
	
	@JsonProperty("error_output")
	private String errorOutput;
	private Status status;
	
	@JsonProperty("error_message")
	private String errorMessage;
	
	public enum Status {
		ok,
		error
	}
	
	public RunResponse(Status status, String exitCode, String standardOutput, String errorOutput,
			String errorMessage) {
		this.setStatus(status);
		this.exitCode = exitCode;
		this.standardOutput = standardOutput;
		this.errorOutput = errorOutput;
		this.errorMessage = errorMessage;
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

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
}