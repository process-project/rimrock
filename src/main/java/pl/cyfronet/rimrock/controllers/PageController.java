package pl.cyfronet.rimrock.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {
	@RequestMapping("/")
	public String main() {
		return "main";
	}
	
	@RequestMapping("/processes")
	public String processes() {
		return "processes";
	}
	
	@RequestMapping("/iprocesses")
	public String iprocesses() {
		return "iprocesses";
	}
	
	@RequestMapping("/jobs")
	public String jobs() {
		return "jobs";
	}
	
	@RequestMapping("/team")
	public String team() {
		return "team";
	}
}