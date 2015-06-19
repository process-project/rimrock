package pl.cyfronet.rimrock.controllers.rest.gridjobs;

import static java.util.Arrays.asList;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.globus.ftp.DataSinkStream;
import org.globus.ftp.DataSourceStream;
import org.globus.ftp.GridFTPClient;
import org.globus.ftp.exception.ClientException;
import org.globus.ftp.exception.ServerException;
import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pl.cyfronet.rimrock.controllers.rest.jobs.JobActionRequest;
import pl.cyfronet.rimrock.gridworkerapi.beans.GridJob;
import pl.cyfronet.rimrock.gridworkerapi.beans.GridJobStatus;
import pl.cyfronet.rimrock.gridworkerapi.service.GridWorkerService;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.GridJobRepository;

@RestController
public class GridJobsController {
	private static final Logger log = LoggerFactory.getLogger(GridJobsController.class);
	
	private GridWorkerService gridWorkerService;
	private ProxyHelper proxyHelper;
	private GridJobRepository gridJobRepository;
	
	@Value("${grid.ftp.host}") private String gridFtpHost;
	
	@Autowired
	public GridJobsController(@Qualifier("jsaga") GridWorkerService gridWorkerService, ProxyHelper proxyHelper, GridJobRepository gridJobRepository) {
		this.gridWorkerService = gridWorkerService;
		this.proxyHelper = proxyHelper;
		this.gridJobRepository = gridJobRepository;
	}
	
	@RequestMapping(value = "/api/gridjobs/gridproxy/{vo:.+}")
	public String extendGridProxy(@RequestHeader("PROXY") String proxy, @PathVariable("vo") String vo) throws RemoteException {
		return gridWorkerService.extendProxy(decodeProxy(proxy), vo);
	}
	
	@RequestMapping(value = "/api/gridjobs", method = POST, consumes = {MULTIPART_FORM_DATA_VALUE, APPLICATION_FORM_URLENCODED_VALUE},
			produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<GLiteJobStatus> submitGridJob(GLiteJobSubmission jobSubmission, @RequestHeader("PROXY") String encodedProxy)
			throws ServerException, IOException, CredentialException, GSSException, ClientException {
		String jobId = UUID.randomUUID().toString();
		GLiteJobStatus result = new GLiteJobStatus();
		Map<String, String> uploadedFiles = null;
		String decodedProxy = decodeProxy(encodedProxy);
		
		if(jobSubmission.getFiles() != null && jobSubmission.getFiles().size() > 0) {
			uploadedFiles = uploadFiles(jobSubmission.getFiles(), decodedProxy, jobId);
		}
		
		GridJob gridJob = new GridJob();
		gridJob.setUserProxy(decodedProxy);
		gridJob.getAttributes().put(GridJob.EXECUTABLE, jobSubmission.getExecutable());
		gridJob.getAttributes().put(GridJob.OUTPUT_FILE_NAME, jobSubmission.getStdOutput());
		gridJob.getAttributes().put(GridJob.ERROR_FILE_NAME, jobSubmission.getStdError());
		gridJob.getAttributes().put(GridJob.MY_PROXY_SERVER, jobSubmission.getMyProxyServer());
		
		if(uploadedFiles != null) {
			for(String fileName : uploadedFiles.keySet()) {
				gridJob.getInputSandbox().put(uploadedFiles.get(fileName), fileName);
			}
		}
		
		if(jobSubmission.getStdOutput() != null) {
			gridJob.getOutputSandbox().put(createGridFtpPath(jobSubmission.getStdOutput(), jobId, decodedProxy), jobSubmission.getStdOutput());
		}
		
		if(jobSubmission.getStdError() != null) {
			gridJob.getOutputSandbox().put(createGridFtpPath(jobSubmission.getStdError(), jobId, decodedProxy), jobSubmission.getStdError());
		}
		
		if(jobSubmission.getOutputSandbox() != null) {
			for(String outputFile : jobSubmission.getOutputSandbox()) {
				gridJob.getOutputSandbox().put(createGridFtpPath(outputFile, jobId, decodedProxy), outputFile);
			}
		}
		
		if(jobSubmission.getArguments() != null) {
			for(String arg : jobSubmission.getArguments()) {
				gridJob.getArguments().add(arg);
			}
		}
		
		if(jobSubmission.getCandidateHosts() != null) {
			for(String candidateHost : jobSubmission.getCandidateHosts()) {
				gridJob.getCandidateHosts().add(candidateHost);
			}
		}
		
		GridJobStatus status = gridWorkerService.submitGridJob(gridJob);
		result.setJobId(jobId);
		result.setNativeJobId(status.getJobId());
		result.setStatus(status.getStatus());
		result.setTag(jobSubmission.getTag());
		log.debug("Submitted job jdl is {}", status.getJdl());
		
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = new pl.cyfronet.rimrock.domain.GridJob();
		dbGridJob.setJobId(jobId);
		dbGridJob.setNativeJobId(status.getJobId());
		dbGridJob.setUserLogin(proxyHelper.getUserLogin(decodedProxy));
		dbGridJob.setJdl(status.getJdl());
		dbGridJob.setTag(jobSubmission.getTag());
		gridJobRepository.save(dbGridJob);
		
		return new ResponseEntity<GLiteJobStatus>(result, CREATED);
	}

	@RequestMapping(value = "/api/gridjobs", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<List<GLiteJobStatus>> getGridJobs(@RequestHeader("PROXY") String encodedProxy,
			@RequestParam(value = "tag", required = false) String tag) throws RemoteException, CredentialException {
		List<GLiteJobStatus> result = new ArrayList<GLiteJobStatus>();
		String decodedProxy = decodeProxy(encodedProxy);
		List<pl.cyfronet.rimrock.domain.GridJob> gridJobs = null;
		
		if(tag != null) {
			gridJobs = gridJobRepository.findByUserLoginAndTag(proxyHelper.getUserLogin(decodedProxy), tag);
		} else {
			gridJobs = gridJobRepository.findByUserLogin(proxyHelper.getUserLogin(decodedProxy));
		}
		
		for(pl.cyfronet.rimrock.domain.GridJob gridJob : gridJobs) {
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(gridJob.getNativeJobId(), decodedProxy);
			GLiteJobStatus jobStatus = new GLiteJobStatus();
			jobStatus.setJobId(gridJob.getJobId());
			jobStatus.setNativeJobId(gridJob.getNativeJobId());
			jobStatus.setStatus(gridJobStatus.getStatus());
			jobStatus.setTag(gridJob.getTag());
			result.add(jobStatus);
		}
		
		return new ResponseEntity<List<GLiteJobStatus>>(result, OK);
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId:.+}", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<GLiteJobStatus> getGridJob(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy) throws RemoteException,
			CredentialException {
		log.debug("Retrieving job info for id {}", jobId);
		String decodedProxy = decodeProxy(encodedProxy);
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(decodedProxy));
		
		if(dbGridJob != null) {
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(dbGridJob.getNativeJobId(), decodedProxy);
			GLiteJobStatus jobStatus = new GLiteJobStatus();
			jobStatus.setJobId(dbGridJob.getJobId());
			jobStatus.setNativeJobId(dbGridJob.getNativeJobId());
			jobStatus.setStatus(gridJobStatus.getStatus());
			
			return new ResponseEntity<GLiteJobStatus>(jobStatus, OK);
		} else {
			return new ResponseEntity<GLiteJobStatus>(NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId}/jdl", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getGridJobJdl(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy) throws RemoteException,
			CredentialException {
		String decodedProxy = decodeProxy(encodedProxy);
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(decodedProxy));
		
		if(dbGridJob != null) {
			return new ResponseEntity<String>(dbGridJob.getJdl(), OK);
		} else {
			return new ResponseEntity<String>(NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId}/files/{fileName:.+}", method = GET)
	public ResponseEntity<InputStreamResource> getFile(@PathVariable("jobId") String jobId, @PathVariable("fileName") String fileName,
				@RequestHeader("PROXY") String encodedProxy)
			throws CredentialException, ServerException, IOException, GSSException, ClientException {
		String decodedProxy = decodeProxy(encodedProxy);
		String userLogin = proxyHelper.getUserLogin(decodedProxy);
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, userLogin);
		
		if(dbGridJob != null) {
			if(dbGridJob.getUserLogin() != null && dbGridJob.getUserLogin().equals(userLogin)) {
				GridFTPClient client = null;
				
				try {
					GSSCredential gsscredential = proxyHelper.getGssCredential(decodedProxy);
					client = new GridFTPClient(gridFtpHost, 2811);
					client.authenticate(gsscredential);
					client.setPassive();
					client.setLocalActive();
					
					String remoteFilePath = createBaseGridFtpDirectory(userLogin) + ".rimrock/" + jobId + "/" + fileName;
					long size = client.getSize(remoteFilePath);
					HttpHeaders headers = new HttpHeaders();
					headers.setContentLength(size);
					headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
					
					PipedOutputStream pos = new PipedOutputStream();
					PipedInputStream pis = new PipedInputStream(pos);
					client.get(remoteFilePath, new DataSinkStream(pos), null);
					
					return new ResponseEntity<InputStreamResource>(new InputStreamResource(pis), headers, OK);
				} finally {
					if(client != null) {
						client.close();
					}
				}
			} else {
				return new ResponseEntity<InputStreamResource>(FORBIDDEN);
			}
		} else {
			return new ResponseEntity<InputStreamResource>(NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId:.+}", method = DELETE)
	public ResponseEntity<Void> deleteGridJob(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy) throws RemoteException,
			CredentialException {
		log.debug("Retrieving job info for id {}", jobId);
		String decodedProxy = decodeProxy(encodedProxy);
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(decodedProxy));
		
		if(dbGridJob != null) {
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(dbGridJob.getNativeJobId(), decodedProxy);
			
			if(!asList("FAILED", "DONE").contains(gridJobStatus.getStatus())) {
				gridWorkerService.abortGridJob(dbGridJob.getNativeJobId(), decodedProxy);
			}

			gridJobRepository.delete(dbGridJob.getId());
			
			return new ResponseEntity<Void>(NO_CONTENT);
		} else {
			return new ResponseEntity<Void>(NOT_FOUND);
		}
	}
	
	@RequestMapping(value = "/api/gridjobs/{jobId:.+}", method = PUT, consumes = APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> abortGridJob(@PathVariable("jobId") String jobId, @RequestHeader("PROXY") String encodedProxy,
			@Valid @RequestBody JobActionRequest actionRequest) throws RemoteException,
			CredentialException {
		log.debug("Retrieving job info for id {}", jobId);
		String decodedProxy = decodeProxy(encodedProxy);
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(decodedProxy));
		
		if(dbGridJob != null) {
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(dbGridJob.getNativeJobId(), decodedProxy);
			
			if(actionRequest.getAction() != null && actionRequest.getAction().equalsIgnoreCase("abort")) {
				if(!asList("FAILED", "DONE").contains(gridJobStatus.getStatus())) {
					gridWorkerService.abortGridJob(dbGridJob.getNativeJobId(), decodedProxy);
				}
			}
			
			return new ResponseEntity<Void>(NO_CONTENT);
		} else {
			return new ResponseEntity<Void>(NOT_FOUND);
		}
	}
	
	private String decodeProxy(String proxy) {
		return proxyHelper.decodeProxy(proxy).trim();
	}
	
	private Map<String, String> uploadFiles(List<MultipartFile> files, String decodedProxy, String jobId) throws CredentialException,
			GSSException, ClientException, ServerException, IOException {
		GridFTPClient client = null;
		Map<String, String> result = new HashMap<>();
		
		try {
			GSSCredential gsscredential = proxyHelper.getGssCredential(decodedProxy);
			client = new GridFTPClient(gridFtpHost, 2811);
			client.authenticate(gsscredential);
			client.setPassive();
			client.setLocalActive();
			client.changeDir(createBaseGridFtpDirectory(proxyHelper.getUserLogin(decodedProxy)));
			
			if(log.isDebugEnabled()) {
				log.debug("Changed working directory to {}", client.getCurrentDir());
			}
			
			if(!client.exists(".rimrock")) {
				client.makeDir(".rimrock");
			}
			
			String jobDirectory = ".rimrock/" + jobId + "/";
			client.makeDir(jobDirectory);
			
			for(MultipartFile file : files) {
				String destinationPath = jobDirectory + file.getOriginalFilename();
				log.debug("Uploading file {} to {}", file.getOriginalFilename(), destinationPath);
				client.put(destinationPath, new DataSourceStream(file.getInputStream()), null);
				client.setPassive();
				client.setLocalActive();
				result.put(file.getOriginalFilename(), createGridFtpPath(file.getOriginalFilename(), jobId, decodedProxy));
			}
			
			client.setPassive();
			client.setLocalActive();
		} finally {
			if(client != null) {
				try {
					client.close();
				} catch(Exception e) {
					log.warn("GridFTP client close error", e);
				}
			}
		}
		
		return result;
	}

	private String createBaseGridFtpDirectory(String userLogin) {
		return "/storage/" + userLogin + "/";
	}
	
	private String createGridFtpPath(String fileName, String jobId, String proxy) throws CredentialException {
		return "gsiftp://" + gridFtpHost + ":2811" + createBaseGridFtpDirectory(proxyHelper.getUserLogin(proxy)) + ".rimrock/" + jobId + "/" + fileName;
	}
}