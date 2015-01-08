package pl.cyfronet.rimrock.controllers.rest.irun;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import pl.cyfronet.rimrock.controllers.exceptions.ResourceNotFoundException;
import pl.cyfronet.rimrock.controllers.rest.PathHelper;
import pl.cyfronet.rimrock.controllers.rest.irun.InteractiveProcessResponse.Status;
import pl.cyfronet.rimrock.controllers.rest.jobs.ValidationException;
import pl.cyfronet.rimrock.domain.InteractiveProcess;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.InteractiveProcessRepository;
import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunException;
import pl.cyfronet.rimrock.services.RunResults;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;

import com.sshtools.j2ssh.util.InvalidStateException;

@RestController
public class InteractiveRunController {
	private static final Logger log = LoggerFactory.getLogger(InteractiveRunController.class);
	
	private InteractiveProcessRepository processRepository;
	private GsisshRunner runner;
	private FileManagerFactory fileManagerFactory;
	private ProxyHelper proxyHelper;
	
	@Value("${irun.timeout.seconds}") private String iprocessTimeoutSeconds;

	@Autowired
	public InteractiveRunController(InteractiveProcessRepository processRepository, GsisshRunner runner, FileManagerFactory fileManagerFactory, ProxyHelper proxyHelper) {
		this.processRepository = processRepository;
		this.runner = runner;
		this.fileManagerFactory = fileManagerFactory;
		this.proxyHelper = proxyHelper;
	}
	
	@RequestMapping(value = "/api/internal/update", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<InternalUpdateResponse> update(@Valid @RequestBody InternalUpdateRequest updateRequest) {
		log.debug("Processing internal update request with body {}", updateRequest);
		
		InteractiveProcess process = getProcess(updateRequest.getProcessId());
		String input = process.getPendingInput();
		process.setPendingInput("");
		process.setFinished(updateRequest.isFinished());
		process.setOutput((process.getOutput() == null ? "" : process.getOutput()) + updateRequest.getStandardOutput());
		process.setError((process.getError() == null ? "" : process.getError()) + updateRequest.getStandardError());
		processRepository.save(process);
		
		return new ResponseEntity<InternalUpdateResponse>(new InternalUpdateResponse(input), OK);
	}
	
	@RequestMapping(value = "/api/iprocess", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<InteractiveProcessResponse> startInteractiveProcess(@RequestHeader("PROXY") String proxy,
			@Valid @RequestBody InteractiveProcessRequest request, BindingResult errors) throws CredentialException, FileManagerException, InvalidStateException,
			KeyStoreException, CertificateException, GSSException, IOException, InterruptedException {
		if(errors.hasErrors()) {
			throw new ValidationException(errors);
		}
		
		String decodedProxy = getDecodedValidatedProxy(proxy);
		FileManager fileManager = fileManagerFactory.get(decodedProxy);
		fileManager.cp(PathHelper.getRootPath(request.getHost(), proxyHelper.getUserLogin(decodedProxy)) + ".rimrock/iwrapper.py", new ClassPathResource("scripts/iwrapper.py"));
		fileManager.cp(PathHelper.getRootPath(request.getHost(), proxyHelper.getUserLogin(decodedProxy)) + ".rimrock/TERENASSLCA", new ClassPathResource("certs/TERENASSLCA"));
		
		String processId = UUID.randomUUID().toString();
		String internalUrl = MvcUriComponentsBuilder.fromMethodCall(on(InteractiveRunController.class).update(null)).build().toUriString();
		log.debug("Attempting to start new interactive process with id {} and reporting URL {}", processId, internalUrl);
		
		RunResults runResults = runner.run(request.getHost(), decodedProxy,
				String.format("module load plgrid/tools/python/3.3.2; (url='%s' processId='%s' command='%s' timeout='%s' nohup python3 .rimrock/iwrapper.py &)",
						internalUrl, processId, request.getCommand(), iprocessTimeoutSeconds), 5000);
		
		if(runResults.isTimeoutOccured() || runResults.getExitCode() != 0) {
			throw new RunException("Interactive process could not be properly executed", runResults);			
		} 
		
		InteractiveProcess interactiveProcess = new InteractiveProcess();
		interactiveProcess.setProcessId(processId);
		interactiveProcess.setUserLogin(proxyHelper.getUserLogin(decodedProxy));
		processRepository.save(interactiveProcess);
		
		InteractiveProcessResponse response = new InteractiveProcessResponse(Status.OK, null);
		response.setProcessId(processId);
		
		return new ResponseEntity<InteractiveProcessResponse>(response, OK);	
	}
	
	@RequestMapping(value = "/api/iprocess", method = GET, produces = APPLICATION_JSON_VALUE)
	@SuppressWarnings({"rawtypes", "unchecked"})
	public ResponseEntity getInteractiveProcesseses(@RequestHeader("PROXY") String proxy, @RequestHeader(required = false, value = "PROCESS-ID") String processId) throws CredentialException, GSSException, KeyStoreException, CertificateException, IOException {
		String decodedProxy = getDecodedValidatedProxy(proxy);
		String userLogin = proxyHelper.getUserLogin(decodedProxy);
		
		if(processId != null) {
			InteractiveProcess process = getProcess(processId);
			
			if(process.getUserLogin() == null || !process.getUserLogin().equals(userLogin)) {
				throw new ResourceAccessException("You do not seem to be the owner of the requested interactive process");
			}
			
			String output = process.getOutput();
			String error = process.getError();
			process.setOutput("");
			process.setError("");
			processRepository.save(process);
			
			InteractiveProcessResponse response = new InteractiveProcessResponse(Status.OK, null);
			response.setStandardOutput(output);
			response.setStandardError(error);
			response.setFinished(process.isFinished());
			response.setProcessId(processId);
			
			return new ResponseEntity<InteractiveProcessResponse>(response, OK);
		} else {
			List<InteractiveProcess> processes = processRepository.findByUserLogin(userLogin);
			
			return new ResponseEntity(processes, OK);
		}
	}
	
	@RequestMapping(value = "/api/iprocess", method = PUT, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<InteractiveProcessResponse> processInteractiveProcessInput(@RequestHeader("PROXY") String proxy, @RequestHeader("PROCESS-ID") String processId,
			@Valid @RequestBody InteractiveProcessInputRequest request, BindingResult errors) throws CredentialException {
		getDecodedValidatedProxy(proxy);
		
		InteractiveProcess process = getProcess(processId);
		String output = process.getOutput();
		String error = process.getError();
		process.setOutput("");
		process.setError("");
		process.setPendingInput(merge(process.getPendingInput(), request.getStandardInput()));
		processRepository.save(process);
		
		InteractiveProcessResponse response = new InteractiveProcessResponse(Status.OK, null);
		response.setStandardOutput(output);
		response.setStandardError(error);
		response.setFinished(process.isFinished());
		response.setProcessId(processId);
		
		return new ResponseEntity<InteractiveProcessResponse>(response, OK);
	}

	private String getDecodedValidatedProxy(String proxy) throws CredentialException {
		String decodedProxy = proxyHelper.decodeProxy(proxy);
		proxyHelper.verify(decodedProxy);

		return decodedProxy;
	}

	private String merge(String first, String second) {
		String normalized = (first == null ? "" : first).trim();
		
		if(!normalized.isEmpty()) {
			normalized += "\n";
		}
		
		return normalized + second.trim();
	}
	
	private InteractiveProcess getProcess(String processId) {
		InteractiveProcess process = processRepository.findByProcessId(processId);
		
		if(process == null) {
			throw new ResourceNotFoundException("Interactive process with id " + processId + " could not be found");
		}
		
		return process;
	}
}
