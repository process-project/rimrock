package pl.cyfronet.rimrock.controllers.rest.gridjobs;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

import org.globus.ftp.exception.ClientException;
import org.globus.ftp.exception.ServerException;
import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pl.cyfronet.rimrock.controllers.rest.jobs.JobActionRequest;
import pl.cyfronet.rimrock.domain.GridJob.Middleware;
import pl.cyfronet.rimrock.gridworkerapi.beans.GridJob;
import pl.cyfronet.rimrock.gridworkerapi.service.JSagaExtras;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.services.gridjob.GridJobHelper;
import pl.cyfronet.rimrock.services.gridjob.GridJobInfo;
import pl.cyfronet.rimrock.services.gridjob.GridJobSubmission;

@RestController
public class GridJobsController {
	private static final Logger log = LoggerFactory.getLogger(GridJobsController.class);
	
	private JSagaExtras gridWorkerService;
	private ProxyHelper proxyHelper;
	private GridJobHelper gridJobHelper;
	
	@Autowired
	public GridJobsController(@Qualifier("jsaga") JSagaExtras gridWorkerService, ProxyHelper proxyHelper, GridJobHelper gridJobHelper) {
		this.gridWorkerService = gridWorkerService;
		this.proxyHelper = proxyHelper;
		this.gridJobHelper = gridJobHelper;
	}
	
	@RequestMapping(value = "/api/gridjobs/gridproxy/{vo:.+}")
	public String extendGridProxy(@RequestHeader("PROXY") String proxy, @PathVariable("vo") String vo) throws RemoteException {
		return gridWorkerService.extendProxy(decodeProxy(proxy), vo);
	}
	
	@RequestMapping(value = "/api/gridjobs",
					method = POST,
					consumes = {MULTIPART_FORM_DATA_VALUE, APPLICATION_FORM_URLENCODED_VALUE},
					produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<GridJobInfo> submitGridJob(GridJobSubmission jobSubmission, @RequestHeader("PROXY") String encodedProxy)
			throws ServerException, IOException, CredentialException, GSSException, ClientException {
		String jobId = gridJobHelper.generateGridJobId();
		String decodedProxy = decodeProxy(encodedProxy);
		Map<String, String> uploadedFiles = new HashMap<String, String>();
		Optional.ofNullable(jobSubmission.getFiles()).ifPresent(files -> {
			if(files.size() > 0) {
				try {
					gridJobHelper.uploadFiles(files, decodedProxy, jobId, uploadedFiles);
				} catch (Exception e) {
					throw new RuntimeException("Files could not be uploaded", e);
				}
			}
		});
		
		GridJob gridJob = gridJobHelper.prepareGridJob(jobSubmission, uploadedFiles, jobId, decodedProxy, false);
		GridJobInfo gridJobInfo = gridJobHelper.submitGridJob(gridJob, decodedProxy, jobId, jobSubmission.getTag(), Middleware.jsaga, gridWorkerService);
		
		return new ResponseEntity<GridJobInfo>(gridJobInfo, CREATED);
	}

	@RequestMapping(value = "/api/gridjobs",
					method = GET,
					produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<List<GridJobInfo>> getGridJobs(@RequestHeader("PROXY") String encodedProxy,
			@RequestParam(value = "tag", required = false) String tag) throws RemoteException, CredentialException {
		String decodedProxy = decodeProxy(encodedProxy);
		
		return new ResponseEntity<List<GridJobInfo>>(gridJobHelper.getGridJobs(gridWorkerService, Middleware.jsaga, decodedProxy, tag), OK);
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<GridJobInfo> getGridJob(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy) throws RemoteException,
			CredentialException {
		log.debug("Retrieving job info for id {}", jobId);
		String decodedProxy = decodeProxy(encodedProxy);
		GridJobInfo gridJobInfo = gridJobHelper.getGridJob(gridWorkerService, decodedProxy, jobId);

		if(gridJobInfo == null) {
			return new ResponseEntity<GridJobInfo>(NOT_FOUND);
		} else {
			return new ResponseEntity<GridJobInfo>(gridJobInfo, OK);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId}/jdl", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getGridJobJdl(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy) throws RemoteException,
			CredentialException {
		String decodedProxy = decodeProxy(encodedProxy);
		String jobDescription = gridJobHelper.getJobDescription(gridWorkerService, decodedProxy, jobId);
		
		if(jobDescription != null) {
			return new ResponseEntity<String>(jobDescription, OK);
		} else {
			return new ResponseEntity<String>(NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId}/files/{fileName:.+}", method = GET)
	public ResponseEntity<InputStreamResource> getFile(@PathVariable("jobId") String jobId, @PathVariable("fileName") String fileName,
				@RequestHeader("PROXY") String encodedProxy)
			throws CredentialException, ServerException, IOException, GSSException, ClientException {
		String decodedProxy = decodeProxy(encodedProxy);
		
		if(gridJobHelper.isGridJobOwned(jobId, decodedProxy)) {
			PipedOutputStream pos = new PipedOutputStream();
			PipedInputStream pis = new PipedInputStream(pos);
			Long fileSize = gridJobHelper.getFile(decodedProxy, jobId, fileName, pos);
			
			if(fileSize != null) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentLength(fileSize);
				headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
				
				return new ResponseEntity<InputStreamResource>(new InputStreamResource(pis), headers, OK);
			} else {
				pis.close();
				
				return new ResponseEntity<InputStreamResource>(NOT_FOUND);
			}
		} else {
			return new ResponseEntity<InputStreamResource>(FORBIDDEN);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId:.+}", method = DELETE)
	public ResponseEntity<Void> deleteGridJob(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy) throws RemoteException,
			CredentialException {
		log.debug("Retrieving job info for id {}", jobId);
		String decodedProxy = decodeProxy(encodedProxy);
		boolean deleted = gridJobHelper.deleteJob(gridWorkerService, decodedProxy, jobId);
		
		if(deleted) {
			return new ResponseEntity<Void>(NO_CONTENT);
		} else {
			return new ResponseEntity<Void>(NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId:.+}",
					method = PUT,
					consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> abortGridJob(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy,
			@Valid @RequestBody JobActionRequest actionRequest) throws RemoteException, CredentialException {
		log.debug("Aborting job for id {}", jobId);
		String decodedProxy = decodeProxy(encodedProxy);
		
		if(actionRequest.getAction() != null && actionRequest.getAction().equalsIgnoreCase("abort")) {
			boolean aborted = gridJobHelper.abortJob(gridWorkerService, decodedProxy, jobId);
			
			if(aborted) {
				return new ResponseEntity<Void>(NO_CONTENT);
			} else {
				return new ResponseEntity<Void>(NOT_FOUND);
			}
		} else {
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
	}
	
	private String decodeProxy(String proxy) {
		return proxyHelper.decodeProxy(proxy).trim();
	}
}