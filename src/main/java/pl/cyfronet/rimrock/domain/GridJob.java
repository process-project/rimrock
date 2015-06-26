package pl.cyfronet.rimrock.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class GridJob {
	@Id	@GeneratedValue
	private Long id;
	
	@Column(length = 102400)
	private String jdl;
	
	private String jobId;
	private String nativeJobId;
	private String userLogin;
	private String tag;
	private Middleware middleware;
	
	public enum Middleware {
		qcg,
		jsaga
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
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
	public String getUserLogin() {
		return userLogin;
	}
	public void setUserLogin(String userLogin) {
		this.userLogin = userLogin;
	}
	public String getJdl() {
		return jdl;
	}
	public void setJdl(String jdl) {
		this.jdl = jdl;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public Middleware getMiddleware() {
		return middleware;
	}
	public void setMiddleware(Middleware middleware) {
		this.middleware = middleware;
	}
}