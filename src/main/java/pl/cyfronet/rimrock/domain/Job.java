package pl.cyfronet.rimrock.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Job {
	@Id	@GeneratedValue
	private Long id;
	
	@Column(nullable = false)
	private String jobId;
	
	@Column(nullable = false)
	private String standardOutputLocation;
	
	@Column(nullable = false)
	private String standardErrorLocation;
	
	@Column(nullable = false)
	private String userLogin;
	
	@Column(nullable = false)
	private String host;
	
	@Column(nullable = false)
	private String status;
	
	@Column(nullable = true)
	private String nodes;
	
	@Column(nullable = true)
	private String cores;
	
	@Column(nullable = true)
	private String wallTime;
	
	@Column(nullable = true)
	private String queueTime;
	
	@Column(nullable = true)
	private String startTime;
	
	@Column(nullable = true)
	private String endTime;
	
	private String tag;

	protected Job() {
		
	}

	public Job(String jobId, String status, String standardOutputLocation,
			String standardErrorLocation, String userLogin, String host, String tag, String nodes,
			String cores, String walltime, String queuetime, String starttime, String endtime) {
		this.jobId = jobId;
		this.status = status;
		this.standardOutputLocation = standardOutputLocation;
		this.standardErrorLocation = standardErrorLocation;
		this.userLogin = userLogin;
		this.host = host;
		this.tag = tag;
		this.nodes = nodes;
		this.cores = cores;
		this.wallTime = walltime;
		this.queueTime = queuetime;
		this.startTime = starttime;
		this.endTime = endtime;
	}

    public Job(String jobId, String status, String standardOutputLocation,
    		String standardErrorLocation, String userLogin, String host, String tag) {
        this(jobId, status, standardOutputLocation, standardErrorLocation, userLogin, host, tag,
        		null, null, null, null, null, null);
    }

	public Long getId() {
		return id;
	}
	
	protected void setId(Long id) {
		this.id = id;
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
	
	public String getUserLogin() {
		return userLogin;
	}

	public void setUserLogin(String userLogin) {
		this.userLogin = userLogin;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Job{" +
				"id=" + id +
				", jobId='" + jobId + '\'' +
				", standardOutputLocation='" + standardOutputLocation + '\'' +
				", standardErrorLocation='" + standardErrorLocation + '\'' +
				", userLogin='" + userLogin + '\'' +
				", host='" + host + '\'' +
				", status='" + status + '\'' +
				", nodes='" + nodes + '\'' +
				", cores='" + cores + '\'' +
				", wallTime='" + wallTime + '\'' +
				", queueTime='" + queueTime + '\'' +
				", startTime='" + startTime + '\'' +
				", endTime='" + endTime + '\'' +
				", tag='" + tag + '\'' +
				'}';
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