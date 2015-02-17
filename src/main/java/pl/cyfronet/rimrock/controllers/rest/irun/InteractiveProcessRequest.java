package pl.cyfronet.rimrock.controllers.rest.irun;

import javax.validation.constraints.NotNull;

public class InteractiveProcessRequest {
	@NotNull private String host;
	@NotNull private String command;
	private String tag;
	
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
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
}