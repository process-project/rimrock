package pl.cyfronet.rimrock.controllers.rest.run;

import javax.validation.constraints.NotNull;


public class RunRequest {
	@NotNull
	private String host;
	private String command;
	@NotNull
	private String proxy;

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
	public String getProxy() {
		return proxy;
	}
	public void setProxy(String proxy) {
		this.proxy = proxy;
	}
	
	@Override
	public String toString() {
		return "RunRequest [host=" + host + ", command=" + command + ", proxy="
				+ proxy + "]";
	}
}