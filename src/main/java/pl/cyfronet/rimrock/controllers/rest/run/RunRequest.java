package pl.cyfronet.rimrock.controllers.rest.run;

import javax.validation.constraints.NotNull;


public class RunRequest {
	@NotNull
	private String host;
	private String command;

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
		return "RunRequest [host=" + host + ", command=" + command + "]";
	}
}