package pl.cyfronet.rimrock.services.job;

import com.fasterxml.jackson.annotation.JsonProperty;

public class History {
	@JsonProperty("job_id") private String jobId;
	@JsonProperty("error_message") private String errorMessage;
	@JsonProperty("job_nodes") private String jobNodes;
	@JsonProperty("job_cores") private String jobCores;
	@JsonProperty("job_walltime") private String jobWalltime;
	@JsonProperty("job_queuetime") private String jobQueuetime;
	@JsonProperty("job_starttime") private String jobStarttime;
	@JsonProperty("job_endtime") private String jobEndtime;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getJobNodes() {
        return jobNodes;
    }

    public void setJobNodes(String jobNodes) {
        this.jobNodes = jobNodes;
    }

    public String getJobCores() {
        return jobCores;
    }

    public void setJobCores(String jobCores) {
        this.jobCores = jobCores;
    }

    public String getJobWalltime() {
        return jobWalltime;
    }

    public void setJobWalltime(String jobWalltime) {
        this.jobWalltime = jobWalltime;
    }

    public String getJobQueuetime() {
        return jobQueuetime;
    }

    public void setJobQueuetime(String jobQueuetime) {
        this.jobQueuetime = jobQueuetime;
    }

    public String getJobStarttime() {
        return jobStarttime;
    }

    public void setJobStarttime(String jobStarttime) {
        this.jobStarttime = jobStarttime;
    }

    public String getJobEndtime() {
        return jobEndtime;
    }

    public void setJobEndtime(String jobEndtime) {
        this.jobEndtime = jobEndtime;
    }

    @Override
    public String toString() {
        return "History{" +
                "jobId='" + jobId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", jobNodes='" + jobNodes + '\'' +
                ", jobCores='" + jobCores + '\'' +
                ", jobWalltime='" + jobWalltime + '\'' +
                ", jobQueuetime='" + jobQueuetime + '\'' +
                ", jobStarttime='" + jobStarttime + '\'' +
                ", jobEndtime='" + jobEndtime + '\'' +
                '}';
    }
}