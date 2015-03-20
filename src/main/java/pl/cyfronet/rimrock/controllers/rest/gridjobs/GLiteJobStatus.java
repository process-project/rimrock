package pl.cyfronet.rimrock.controllers.rest.gridjobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GLiteJobStatus {
	@JsonProperty("job_id")
	private String jobId;
	
	@JsonProperty("native_job_id")
	private String nativeJobId;
	
	private String status;
	
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public String getNativeJobId() {
		return nativeJobId;
	}
	public void setNativeJobId(String nativeJobId) {
		this.nativeJobId = nativeJobId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
}