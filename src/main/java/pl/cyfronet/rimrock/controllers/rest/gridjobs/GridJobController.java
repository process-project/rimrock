package pl.cyfronet.rimrock.controllers.rest.gridjobs;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.rmi.RemoteException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.cyfronet.rimrock.gridworkerapi.beans.GridJobStatus;
import pl.cyfronet.rimrock.gridworkerapi.service.GridWorkerService;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

@RestController
public class GridJobController {
	private static final Logger log = LoggerFactory.getLogger(GridJobController.class);
	
	private GridWorkerService gridWorkerService;
	private ProxyHelper proxyHelper;
	
	@Autowired
	public GridJobController(GridWorkerService gridWorkerService, ProxyHelper proxyHelper) {
		this.gridWorkerService = gridWorkerService;
		this.proxyHelper = proxyHelper;
	}
	
	@RequestMapping(value = "/api/gridjobs/gridproxy/{vo:.+}")
	public String extendGridProxy(@RequestHeader("PROXY") String proxy, @PathVariable("vo") String vo) throws RemoteException {
		return gridWorkerService.extendProxy(decodeProxy(proxy), vo);
	}
	
	@RequestMapping(value = "/api/gridjobs", method = POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String submitGridJob() {
		
		return null;
	}
	
	@RequestMapping(value = "/api/gridjobs", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<GridJobStatus>> getGridJobs(@RequestHeader("PROXY") String proxy) throws RemoteException {
		return new ResponseEntity<List<GridJobStatus>>(gridWorkerService.getJobsStatus(decodeProxy(proxy)), OK);
	}
	
	@RequestMapping(value = "/api/gridjobs/{gridJobId:.+}", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GridJobStatus> getGridJob(@PathVariable("gridJobId") String gridJobId, @RequestHeader("PROXY") String proxy) throws RemoteException {
		log.debug("Retrieving job info for id {}", gridJobId);
		
		return new ResponseEntity<GridJobStatus>(gridWorkerService.getGridJobStatus(gridJobId, decodeProxy(proxy)), OK);
	}
	
	private String decodeProxy(String proxy) {
		return proxyHelper.decodeProxy(proxy).trim();
	}
}