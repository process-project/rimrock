package pl.cyfronet.rimrock.controllers;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
public class PageController {
	private static final Logger log = LoggerFactory.getLogger(PageController.class);
	
	@RequestMapping("/")
	public String main(Model model, HttpServletRequest request) {
		fillLocale(model, request);
		log.debug("Debug message");
		
		return "main";
	}
	
	@RequestMapping("/processes")
	public String processes(Model model, HttpServletRequest request) {
		fillLocale(model, request);
		
		return "processes";
	}
	
	@RequestMapping("/jobs")
	public String jobs(Model model, HttpServletRequest request) {
		fillLocale(model, request);
		
		return "jobs";
	}
	
	@RequestMapping("/team")
	public String team(Model model, HttpServletRequest request) {
		fillLocale(model, request);
		
		return "team";
	}
	
	private void fillLocale(Model model, HttpServletRequest request) {
		Locale locale = RequestContextUtils.getLocale(request);
		model.addAttribute("lang", locale.getLanguage());		
	}
}