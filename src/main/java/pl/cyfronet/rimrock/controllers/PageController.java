package pl.cyfronet.rimrock.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {
	private static final Logger log = LoggerFactory.getLogger(PageController.class);
	
	@RequestMapping("/")
	public String main() {
		return "main";
	}
}