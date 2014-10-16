package pl.cyfronet.rimrock.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Job {
	@Id	@GeneratedValue	private Long id;
	@Column(nullable = false) private String jobId;
	@Column(nullable = false) private String standardOutputLocation;
	@Column(nullable = false) private String standardErrorLocation;
	@Column(nullable = false) private String user;
	@Column(nullable = false) private String host;
	@Column(nullable = false) private String status;

	protected Job() {
	}
	
	public Job(String jobId, String status, String standardOutputLocation, String standardErrorLocation, String user, String host) {
		this.jobId = jobId;
		this.status = status;
		this.standardOutputLocation = standardOutputLocation;
		this.standardErrorLocation = standardErrorLocation;
		this.user = user;
		this.host = host;
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
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
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
		return "Job [id=" + id + ", jobId=" + jobId + ", user=" + user
				+ ", host=" + host + ", status=" + status + "]";
	}
}