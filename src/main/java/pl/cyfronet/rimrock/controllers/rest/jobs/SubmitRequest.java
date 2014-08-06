package pl.cyfronet.rimrock.controllers.rest.jobs;

public class SubmitRequest {
	private String host;
	private String script;
	private String proxy;
	
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
}