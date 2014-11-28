package pl.cyfronet.rimrock.controllers;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
public class PageController {
	@RequestMapping("/")
	public String main(Model model, HttpServletRequest request) {
		fillLocale(model, request);
		return "main";
	}
	
	@RequestMapping("/processes")
	public String processes(Model model, HttpServletRequest request) {
		fillLocale(model, request);
		return "processes";
	}
	
	@RequestMapping("/iprocesses")
	public String iprocesses(Model model, HttpServletRequest request) {
		fillLocale(model, request);
		return "iprocesses";
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