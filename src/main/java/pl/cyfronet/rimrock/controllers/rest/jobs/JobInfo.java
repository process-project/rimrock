package pl.cyfronet.rimrock.controllers.rest.jobs;

import pl.cyfronet.rimrock.domain.Job;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobInfo {
	@JsonProperty("job_id")
	private String jobId;

	@JsonProperty("stdout_path")
	private String stdOutPath;

	@JsonProperty("stderr_path")
	private String stdErrPath;

	@JsonProperty("status")
	private String status;
	private String tag;

	public JobInfo() {
	}

	public JobInfo(Job job, String plgdataUrl) {
		this.jobId = job.getJobId();
		this.status = job.getStatus();
		this.tag = job.getTag();
		this.stdOutPath = downloadUrl(plgdataUrl,
				job.getStandardOutputLocation());
		this.stdErrPath = downloadUrl(plgdataUrl,
				job.getStandardErrorLocation());
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getStdOutPath() {
		return stdOutPath;
	}

	public void setStdOutPath(String stdOutPath) {
		this.stdOutPath = stdOutPath;
	}

	public String getStdErrPath() {
		return stdErrPath;
	}

	public void setStdErrPath(String stdErrPath) {
		this.stdErrPath = stdErrPath;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	private String downloadUrl(String plgdataUrl, String filePath) {
		return String.format("%s/download/%s", plgdataUrl, filePath);
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}