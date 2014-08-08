package pl.cyfronet.rimrock.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = "jobs")
public class Job implements Serializable {
	private static final long serialVersionUID = -7439503207454120594L;
	
	@Id	@GeneratedValue	private Long id;
	@Column(nullable = false) private String jobId;
	@Column(nullable = false) private String standardOutputLocation;
	@Column(nullable = false) private String standardErrorLocation;
	@Column(nullable = false) private String user;
	@Column(nullable = false) private String host;

	protected Job() {
	}
	
	public Job(String jobId, String standardOutputLocation, String standardErrorLocation, String user, String host) {
		this.jobId = jobId;
		this.standardOutputLocation = standardOutputLocation;
		this.standardErrorLocation = standardErrorLocation;
		this.user = user;
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
}