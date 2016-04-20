package pl.cyfronet.rimrock.controllers.rest;

import java.util.Arrays;
import java.util.List;

public class PathHelper {
	private List<String> zeusAliases;
	
	private List<String> prometheusAliases;
	
	private String host;
	
	private String userLogin;

	public PathHelper(String host, String userLogin) {
		this.host = host;
		this.userLogin = userLogin;
		zeusAliases = Arrays.asList("zeus.cyfronet.pl", "ui.cyfronet.pl");
		prometheusAliases = Arrays.asList("login01.prometheus.cyf-kr.edu.pl",
				"prometheus.cyf-kr.edu.pl", "login01.prometheus.cyfronet.pl",
				"prometheus.cyfronet.pl");
	}

	public String getTransferPath() {
		String rootPath = getFileRootPath();
		
		if (prometheusAliases.contains(host)) {
			return getHostPrefix() + rootPath;
		} else {
			return rootPath;
		}
	}

	private String getHostPrefix() {
		if (prometheusAliases.contains(host)) {
				return "/prometheus";
		} else {
				return "";
		}
	}

	public String getFileRootPath() {
		if (zeusAliases.contains(host)) {
			return "/people/" + userLogin + "/";
		} else if (prometheusAliases.contains(host)) {
			return "/net/people/" + userLogin + "/";
		} else {
			return "/tmp/";
		}
	}

	public String addHostPrefix(String absolutePath) {
		if (absolutePath != null) {
			if (prometheusAliases.contains(host)) {
				return getHostPrefix() + absolutePath;
			} else {
				return absolutePath;
			}
		} else {
			return null;
		}
	}
}