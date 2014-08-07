package pl.cyfronet.rimrock.controllers.rest.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitRequest {
	private String host;
	private String script;
	@JsonProperty(value = "working_directory") private String workingDirectory;
	
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
	public String getWorkingDirectory() {
		return workingDirectory;
	}
	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
	
	@Override
	public String toString() {
		return "SubmitRequest [host=" + host + ", script=" + script + ", workingDirectory=" + workingDirectory + "]";
	}
}