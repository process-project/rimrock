package pl.cyfronet.rimrock.controllers.rest.irun;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InteractiveProcessResponse {
	@JsonProperty("standard_output") private String standardOutput;
	@JsonProperty("standard_error") private String standardError;
	@JsonProperty("process_id") private String processId;
	@JsonProperty("error_message") private String errorMessage;
	private Status status;
	private boolean finished;
	
	public enum Status {
		OK,
		ERROR
	}
	
	public InteractiveProcessResponse(Status status, String errorMessage) {
		this.status = status;
		this.errorMessage = errorMessage;
	}

	public String getStandardOutput() {
		return standardOutput;
	}

	public void setStandardOutput(String standardOutput) {
		this.standardOutput = standardOutput;
	}

	public String getStandardError() {
		return standardError;
	}

	public void setStandardError(String standardError) {
		this.standardError = standardError;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
}