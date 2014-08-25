package pl.cyfronet.rimrock.controllers.rest;

import pl.cyfronet.rimrock.services.job.RunException;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RunResponse {
	@JsonProperty("exit_code")
	private int exitCode;

	@JsonProperty("standard_output")
	private String standardOutput;

	@JsonProperty("error_output")
	private String errorOutput;
	private Status status;

	@JsonProperty("error_message")
	private String errorMessage;

	public enum Status {
		OK, ERROR
	}	
	
	public RunResponse(Status status, int exitCode, String standardOutput,
			String errorOutput, String errorMessage) {
		this.setStatus(status);
		this.exitCode = exitCode;
		this.standardOutput = standardOutput;
		this.errorOutput = errorOutput;
		this.errorMessage = errorMessage;
	}

	public RunResponse(String errorMessage) {
		this(Status.ERROR, 0, null, null, errorMessage);
	}
	
	public RunResponse(RunException e) {
		this(Status.ERROR, e.getExitCode(), e.getOutput(), e
				.getError(), e.getMessage());
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

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
}