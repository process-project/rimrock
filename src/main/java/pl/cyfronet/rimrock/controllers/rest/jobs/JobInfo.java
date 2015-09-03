package pl.cyfronet.rimrock.controllers.rest.jobs;

import com.fasterxml.jackson.annotation.JsonInclude;
import pl.cyfronet.rimrock.domain.Job;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
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

	private String nodes;
	private String cores;
	@JsonProperty("wall_time")
	private String wallTime;
	@JsonProperty("queue_time")
	private String queueTime;
	@JsonProperty("start_time")
	private String startTime;
	@JsonProperty("end_time")
	private String endTime;


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
		this.nodes = job.getNodes();
		this.cores = job.getCores();
		this.wallTime = job.getWallTime();
		this.queueTime = job.getQueueTime();
		this.startTime = job.getStartTime();
		this.endTime = job.getEndTime();
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

	public String getNodes() {
		return nodes;
	}

	public void setNodes(String nodes) {
		this.nodes = nodes;
	}

	public String getCores() {
		return cores;
	}

	public void setCores(String cores) {
		this.cores = cores;
	}

	public String getWallTime() {
		return wallTime;
	}

	public void setWallTime(String wallTime) {
		this.wallTime = wallTime;
	}

	public String getQueueTime() {
		return queueTime;
	}

	public void setQueueTime(String queueTime) {
		this.queueTime = queueTime;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
}