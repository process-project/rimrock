package pl.cyfronet.rimrock.controllers.rest.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitRequest {
	private String host;
	private String script;
	private String proxy;
	
	@JsonProperty(value = "working_directory")
	private String workingDirectory;
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getScript() {
		return script;
	}
	public void setScript(String script) {
		this.script = script;
	}
	public String getProxy() {
		return proxy;
	}
	public void setProxy(String proxy) {
		this.proxy = proxy;
	}
	public String getWorkingDirectory() {
		return workingDirectory;
	}
	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
	
	@Override
	public String toString() {
		return "SubmitRequest [host=" + host + ", script=" + script + ", proxy=" + proxy + ", workingDirectory=" + workingDirectory + "]";
	}
}