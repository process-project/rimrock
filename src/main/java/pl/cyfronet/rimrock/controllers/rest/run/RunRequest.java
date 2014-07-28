package pl.cyfronet.rimrock.controllers.rest.run;

public class RunRequest {
	private String host;
	private int port;
	private String command;
	private String proxy;
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
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
		return "RunRequest [host=" + host + ", port=" + port + ", command="
				+ command + ", proxy=" + proxy + "]";
	}
}