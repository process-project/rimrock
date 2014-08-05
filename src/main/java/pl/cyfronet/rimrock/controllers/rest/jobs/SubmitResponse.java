package pl.cyfronet.rimrock.controllers.rest.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitResponse {
	private String status;
	
	@JsonProperty("error_message")
	private String errorMessage;
	
	@JsonProperty("job_id")
	private String jobId;
	
	public SubmitResponse(String status, String errorMessage, String jobId) {
		this.status = status;
		this.errorMessage = errorMessage;
		this.jobId = jobId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
}