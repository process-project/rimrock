package pl.cyfronet.rimrock.controllers.rest.irun;

import javax.validation.constraints.NotNull;

public class InteractiveProcessRequest {
	@NotNull private String host;
	@NotNull private String command;
	
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
}