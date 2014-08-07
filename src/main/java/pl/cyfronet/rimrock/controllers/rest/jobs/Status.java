package pl.cyfronet.rimrock.controllers.rest.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Status {
	@JsonProperty("job_id") private String jobId;
	@JsonProperty("job_state") private String status;
	@JsonProperty("error_message") private String errorMessage;
	
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