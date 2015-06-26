package pl.cyfronet.rimrock.services.gridjob;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GridJobInfo {
	@JsonProperty("job_id")
	private String jobId;
	@JsonProperty("native_job_id")
	private String nativeJobId;
	private String status;
	private String tag;
	private String error;
	
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
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
}