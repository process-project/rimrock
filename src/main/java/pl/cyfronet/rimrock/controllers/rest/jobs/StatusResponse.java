package pl.cyfronet.rimrock.controllers.rest.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatusResponse {
	@JsonProperty("job_id") private String jobId;
	@JsonProperty("error_message") private String errorMessage;
	private String status;
	
	public StatusResponse(String jobId, String status, String errorMessage) {
		this.jobId = jobId;
		this.status = status;
		this.errorMessage = errorMessage;
		
	}
	
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
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
}