package pl.cyfronet.rimrock.controllers.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RestController {
	@RequestMapping(value = "run")
	@ResponseBody
	public String run(RunRequest runRequest) {
		return "hello";
	}
}