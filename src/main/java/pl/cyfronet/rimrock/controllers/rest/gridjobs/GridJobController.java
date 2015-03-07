package pl.cyfronet.rimrock.controllers.rest.gridjobs;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GridJobController {
	@Autowired ApplicationContext applicationContext;
	
	@RequestMapping(value = "/api/gridjobs", method = POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String submitGridJob() {
		
		return null;
	}
	
	@RequestMapping(value = "/api/gridjobs", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getGridJobs() {
		
		return null;
	}
	
	@RequestMapping(value = "/api/gridjobs/{gridJobId}", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getGridJob(@PathVariable("gridJobId") String gridJobId) {
		
		return null;
	}
}