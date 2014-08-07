package pl.cyfronet.rimrock.controllers.rest.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitResult {
	private String result;
	@JsonProperty("job_id")
	private String jobId;
	@JsonProperty("standard_output")
	private String standardOutputLocation;
	@JsonProperty("standard_error")
	private String standardErrorLocation;
	@JsonProperty("error_message")
	private String errorMessage;
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public String getStandardOutputLocation() {
		return standardOutputLocation;
	}
	public void setStandardOutputLocation(String standardOutputLocation) {
		this.standardOutputLocation = standardOutputLocation;
	}
	public String getStandardErrorLocation() {
		return standardErrorLocation;
	}
	public void setStandardErrorLocation(String standardErrorLocation) {
		this.standardErrorLocation = standardErrorLocation;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	@Override
	public String toString() {
		return "SubmitResult [result=" + result + ", jobId=" + jobId + ", standardOutputLocation=" + standardOutputLocation + ", standardErrorLocation="
				+ standardErrorLocation + ", errorMessage=" + errorMessage + "]";
	}
}