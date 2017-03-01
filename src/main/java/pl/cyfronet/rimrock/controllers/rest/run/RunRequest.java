package pl.cyfronet.rimrock.controllers.rest.run;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;


public class RunRequest {

	@NotNull
	private String host;

	private String command;

	@JsonProperty("working_directory")
	private String workingDirectory;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	public String toString() {
		return "RunRequest [host=" + host + ", command=" + command + ", workingDirectory="
				+ workingDirectory + "]";
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
}
