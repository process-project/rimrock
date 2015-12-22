package pl.cyfronet.rimrock.controllers.rest;


public class PathHelper {

	private String host;
	private String userLogin;

	public PathHelper(String host, String userLogin) {
		this.host = host;
		this.userLogin = userLogin;
	}

	public String getTransferPath() {
		String rootPath = getFileRootPath();
		switch(host.trim()) {
			case "login01.prometheus.cyf-kr.edu.pl":
			case "prometheus.cyf-kr.edu.pl":
			case "login01.prometheus.cyfronet.pl":
			case "prometheus.cyfronet.pl":
				return getHostPrefix() + rootPath;
			default:
				return rootPath;
		}
	}

	private String getHostPrefix() {
		switch (host.trim()) {
			case "login01.prometheus.cyf-kr.edu.pl":
			case "prometheus.cyf-kr.edu.pl":
			case "login01.prometheus.cyfronet.pl":
			case "prometheus.cyfronet.pl":
				return "/prometheus";
			default:
				return "";
		}
	}

	public String getFileRootPath() {
		switch (host.trim()) {
			case "zeus.cyfronet.pl":
			case "ui.cyfronet.pl":
				return "/people/" + userLogin + "/";
			case "login01.prometheus.cyf-kr.edu.pl":
			case "prometheus.cyf-kr.edu.pl":
			case "login01.prometheus.cyfronet.pl":
			case "prometheus.cyfronet.pl":
				return "/net/people/" + userLogin + "/";
			default:
				return "/tmp/";
		}
	}
}