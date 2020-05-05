package pl.cyfronet.rimrock.controllers.rest;

import java.util.regex.Pattern;

public class PathHelper {
	private Pattern prometheusPattern;
	private Pattern zeusPattern;
	
	private String host;
	
	private String userLogin;

	public PathHelper(String host, String userLogin) {
		this.host = host;
		this.userLogin = userLogin;
	    prometheusPattern = Pattern.compile("^(login\\d+\\.)?pro(metheus)?\\.cyf(ronet|-kr\\.edu)\\.pl$");
	    zeusPattern = Pattern.compile("^(ui|zeus)\\.cyf(ronet|-kr\\.edu)\\.pl$");
	}

	public String getTransferPath() {
		String rootPath = getFileRootPath();
		
		if (prometheusPattern.matcher(host).matches()) {
			return getHostPrefix() + rootPath;
		} else {
			return rootPath;
		}
	}

	private String getHostPrefix() {
		if (prometheusPattern.matcher(host).matches()) {
				return "/prometheus";
		} else {
				return "";
		}
	}

	public String getFileRootPath() {
		if (zeusPattern.matcher(host).matches()) {
			return "/people/" + userLogin + "/";
		} else if (prometheusPattern.matcher(host).matches()) {
			return "/net/people/" + userLogin + "/";
		} else {
			return "/tmp/";
		}
	}

	public String addHostPrefix(String absolutePath) {
		if (absolutePath != null) {
			if (prometheusPattern.matcher(host).matches()) {
				return getHostPrefix() + absolutePath;
			} else {
				return absolutePath;
			}
		} else {
			return null;
		}
	}
}
