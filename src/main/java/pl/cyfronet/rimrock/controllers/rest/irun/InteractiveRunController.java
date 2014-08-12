package pl.cyfronet.rimrock.controllers.rest.irun;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class InteractiveRunController {
	private static final Logger log = LoggerFactory.getLogger(InteractiveRunController.class);
	
	@RequestMapping(value = "/internal/update")
	public ResponseEntity<UpdateResponse> update(@Valid @RequestBody UpdateRequest updateRequest) {
		log.info("Processing internal update request with body {}", updateRequest);
		
		return new ResponseEntity<UpdateResponse>(new UpdateResponse("echo 4"), HttpStatus.OK);
	}
}