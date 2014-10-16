package pl.cyfronet.rimrock.controllers.rest;


public class PathHelper {
	public static String getRootPath(String host, String userLogin) {
		switch (host.trim()) {
			case "zeus.cyfronet.pl":
			case "ui.cyfronet.pl":
				return "/people/" + userLogin + "/";
			default:
				return "/tmp/";
		}
	}
}