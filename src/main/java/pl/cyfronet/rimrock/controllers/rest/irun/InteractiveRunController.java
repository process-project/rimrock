package pl.cyfronet.rimrock.controllers.rest.irun;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static pl.cyfronet.rimrock.controllers.rest.irun.InteractiveProcessResponse.Status.ERROR;

import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import pl.cyfronet.rimrock.controllers.rest.PathHelper;
import pl.cyfronet.rimrock.controllers.rest.RestHelper;
import pl.cyfronet.rimrock.controllers.rest.irun.InteractiveProcessResponse.Status;
import pl.cyfronet.rimrock.domain.InteractiveProcess;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.InteractiveProcessRepository;
import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunResults;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;

@RestController
public class InteractiveRunController {
	private static final Logger log = LoggerFactory.getLogger(InteractiveRunController.class);
	
	private InteractiveProcessRepository processRepository;
	private GsisshRunner runner;
	private FileManagerFactory fileManagerFactory;
	private ProxyHelper proxyHelper;

	@Autowired
	public InteractiveRunController(InteractiveProcessRepository processRepository, GsisshRunner runner, FileManagerFactory fileManagerFactory, ProxyHelper proxyHelper) {
		this.processRepository = processRepository;
		this.runner = runner;
		this.fileManagerFactory = fileManagerFactory;
		this.proxyHelper = proxyHelper;
	}
	
	@RequestMapping(value = "/api/internal/update", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<InternalUpdateResponse> update(@Valid @RequestBody InternalUpdateRequest updateRequest) {
		log.info("Processing internal update request with body {}", updateRequest);
		
		InteractiveProcess process = processRepository.findByProcessId(updateRequest.getProcessId());
		
		if(process != null) {
			String input = process.getPendingInput();
			process.setPendingInput("");
			process.setFinished(updateRequest.isFinished());
			process.setOutput((process.getOutput() == null ? "" : process.getOutput()) + updateRequest.getStandardOutput());
			process.setError((process.getError() == null ? "" : process.getError()) + updateRequest.getStandardError());
			processRepository.save(process);
			
			return new ResponseEntity<InternalUpdateResponse>(new InternalUpdateResponse(input), OK);
		} else {
			return new ResponseEntity<InternalUpdateResponse>(new InternalUpdateResponse(null), INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/api/iprocess", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<InteractiveProcessResponse> startInteractiveProcess(@RequestHeader("PROXY") String proxy,
			@Valid @RequestBody InteractiveProcessRequest request, BindingResult errors) {
		if(errors.hasErrors()) {
			return new ResponseEntity<InteractiveProcessResponse>(new InteractiveProcessResponse(ERROR, RestHelper.convertErrors(errors)), UNPROCESSABLE_ENTITY);
		}
		
		try {
			String decodedProxy = proxyHelper.decodeProxy(proxy);
			FileManager fileManager = fileManagerFactory.get(decodedProxy);
			fileManager.cp(PathHelper.getRootPath(request.getHost(), proxyHelper.getUserLogin(decodedProxy)) + ".rimrock/iwrapper.py", new ClassPathResource("scripts/iwrapper.py"));
			
			String processId = UUID.randomUUID().toString();
			RunResults runResults = runner.run(request.getHost(), decodedProxy,
					String.format("module load plgrid/tools/python/3.3.2; (nohup python3 .rimrock/iwrapper.py %s %s %s &)",
							MvcUriComponentsBuilder.fromMethodCall(on(InteractiveRunController.class).update(null)).build().toUriString(),
							processId, request.getCommand()), 5000);
			
			if(runResults.isTimeoutOccured() || runResults.getExitCode() != 0) {
				return new ResponseEntity<InteractiveProcessResponse>(new InteractiveProcessResponse(Status.ERROR, "Interactive process could not be properly executed"), INTERNAL_SERVER_ERROR);
			} else {
				InteractiveProcess interactiveProcess = new InteractiveProcess();
				interactiveProcess.setProcessId(processId);
				processRepository.save(interactiveProcess);
				
				InteractiveProcessResponse response = new InteractiveProcessResponse(Status.OK, null);
				response.setProcessId(processId);
				
				return new ResponseEntity<InteractiveProcessResponse>(response, OK);
			}
		} catch(Throwable e) {
			log.error("Error", e);
			
			return new ResponseEntity<InteractiveProcessResponse>(new InteractiveProcessResponse(Status.ERROR, e.getMessage()), INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/api/iprocess/{processId}", method = GET, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<InteractiveProcessResponse> getInteractiveProcessStatus(@RequestHeader("PROXY") String proxy, @PathVariable("processId") String processId) {
		InteractiveProcess process = processRepository.findByProcessId(processId);
		
		if(process == null) {
			InteractiveProcessResponse response = new InteractiveProcessResponse(Status.ERROR,
					String.format("Interactive process with id %s cannot be  found", processId));
			response.setProcessId(processId);
			
			return new ResponseEntity<InteractiveProcessResponse>(response, NOT_FOUND);
		} else {
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
		}
	}
	
	@RequestMapping(value = "/api/iprocess/{processId}", method = PUT, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<InteractiveProcessResponse> processInteractiveProcessInput(@RequestHeader("PROXY") String proxy, @PathVariable("processId") String processId,
			@Valid @RequestBody InteractiveProcessInputRequest request, BindingResult errors) {
		InteractiveProcess process = processRepository.findByProcessId(processId);
		
		if(process == null) {
			return new ResponseEntity<InteractiveProcessResponse>(new InteractiveProcessResponse(Status.ERROR,
					String.format("Interactive process with id %s cannot be  found", processId)), NOT_FOUND);
		} else {
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
	}

	private String merge(String first, String second) {
		String normalized = (first == null ? "" : first).trim();
		
		if(!normalized.isEmpty()) {
			normalized += "\n";
		}
		
		return normalized + second.trim();
	}
}