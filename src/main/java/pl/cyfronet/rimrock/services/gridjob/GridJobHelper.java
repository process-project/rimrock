package pl.cyfronet.rimrock.services.gridjob;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import pl.cyfronet.rimrock.gridworkerapi.beans.GridJob;
import pl.cyfronet.rimrock.gridworkerapi.beans.GridJobStatus;
import pl.cyfronet.rimrock.gridworkerapi.service.GridWorkerService;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.GridJobRepository;

@Service
public class GridJobHelper {
	private static final Logger log = LoggerFactory.getLogger(GridJobHelper.class);
	
	private ProxyHelper proxyHelper;
	private GridJobRepository gridJobRepository;
	
	@Value("${grid.ftp.host}") private String gridFtpHost;

	
	@Autowired
	public GridJobHelper(ProxyHelper proxyHelper, GridJobRepository gridJobRepository) {
		this.proxyHelper = proxyHelper;
		this.gridJobRepository = gridJobRepository;
	}
	
	public String generateGridJobId() {
		return UUID.randomUUID().toString();
	}
	
	public String buildJobStagingDirectoryUrl(String proxyValue, String jobId) throws CredentialException {
		return "gsiftp://" + gridFtpHost + ":2811" + createBaseGridFtpDirectory(proxyHelper.getUserLogin(proxyValue)) + ".rimrock/" + jobId + "/";
	}
	
	public void uploadFiles(List<MultipartFile> files, String proxyValue, String jobId, Map<String, String> uploadedFiles) throws CredentialException, GSSException,
			ServerException, IOException, ClientException {
		GridFTPClient client = null;
		
		try {
			GSSCredential gsscredential = proxyHelper.getGssCredential(proxyValue);
			client = new GridFTPClient(gridFtpHost, 2811);
			client.authenticate(gsscredential);
			client.setPassive();
			client.setLocalActive();
			client.changeDir(createBaseGridFtpDirectory(proxyHelper.getUserLogin(proxyValue)));
			
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
				uploadedFiles.put(file.getOriginalFilename(), buildJobStagingDirectoryUrl(proxyValue, jobId) + file.getOriginalFilename());
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
	}
	
	public GridJob prepareGridJob(GridJobSubmission jobSubmission, Map<String, String> uploadedFiles, String jobId, String proxyValue, boolean skipSandbox) throws
			CredentialException {
		GridJob gridJob = new GridJob();
		gridJob.setUserProxy(proxyValue);
		gridJob.getAttributes().put(GridJob.EXECUTABLE, jobSubmission.getExecutable());
		gridJob.getAttributes().put(GridJob.OUTPUT_FILE_NAME, jobSubmission.getStdOutput());
		gridJob.getAttributes().put(GridJob.ERROR_FILE_NAME, jobSubmission.getStdError());
		gridJob.getAttributes().put(GridJob.MY_PROXY_SERVER, jobSubmission.getMyProxyServer());
		
		if(uploadedFiles != null) {
			for(String fileName : uploadedFiles.keySet()) {
				if(skipSandbox && fileName.equals(jobSubmission.getExecutable())) {
					//skipping executable file transfer
					continue;
				}
				
				gridJob.getInputSandbox().put(uploadedFiles.get(fileName), fileName);
			}
		}
		
		if(jobSubmission.getStdOutput() != null && !skipSandbox) {
			gridJob.getOutputSandbox().put(buildJobStagingDirectoryUrl(proxyValue, jobId) + jobSubmission.getStdOutput(), jobSubmission.getStdOutput());
		}
		
		if(jobSubmission.getStdError() != null && !skipSandbox) {
			gridJob.getOutputSandbox().put(buildJobStagingDirectoryUrl(proxyValue, jobId) + jobSubmission.getStdError(), jobSubmission.getStdError());
		}
		
		if(jobSubmission.getOutputSandbox() != null) {
			for(String outputFile : jobSubmission.getOutputSandbox()) {
				gridJob.getOutputSandbox().put(buildJobStagingDirectoryUrl(proxyValue, jobId) + outputFile, outputFile);
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
		
		return gridJob;
	}

	public GridJobInfo submitGridJob(GridJob gridJob, String proxyValue, String jobId, String tag, GridWorkerService gridWorkerService) throws
			CredentialException, RemoteException {
		GridJobInfo result = new GridJobInfo();
		GridJobStatus status = gridWorkerService.submitGridJob(gridJob);
		result.setJobId(jobId);
		result.setNativeJobId(status.getJobId());
		result.setStatus(status.getStatus());
		result.setTag(tag);
		log.debug("Submitted job jdl is {}", status.getJdl());
		
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = new pl.cyfronet.rimrock.domain.GridJob();
		dbGridJob.setJobId(jobId);
		dbGridJob.setNativeJobId(status.getJobId());
		dbGridJob.setUserLogin(proxyHelper.getUserLogin(proxyValue));
		dbGridJob.setJdl(status.getJdl());
		dbGridJob.setTag(tag);
		gridJobRepository.save(dbGridJob);
		
		return result;
	}

	public List<GridJobInfo> getGridJobs(GridWorkerService gridWorkerService, String decodedProxy, String tag) throws CredentialException, RemoteException {
		List<GridJobInfo> result = new ArrayList<>();
		List<pl.cyfronet.rimrock.domain.GridJob> gridJobs = null;
		
		if(tag != null) {
			gridJobs = gridJobRepository.findByUserLoginAndTag(proxyHelper.getUserLogin(decodedProxy), tag);
		} else {
			gridJobs = gridJobRepository.findByUserLogin(proxyHelper.getUserLogin(decodedProxy));
		}
		
		for(pl.cyfronet.rimrock.domain.GridJob gridJob : gridJobs) {
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(gridJob.getNativeJobId(), decodedProxy);
			GridJobInfo jobStatus = new GridJobInfo();
			jobStatus.setJobId(gridJob.getJobId());
			jobStatus.setNativeJobId(gridJob.getNativeJobId());
			jobStatus.setStatus(gridJobStatus.getStatus());
			jobStatus.setTag(gridJob.getTag());
			result.add(jobStatus);
		}
		
		return result;
	}

	public GridJobInfo getGridJob(GridWorkerService gridWorkerService, String proxyValue, String jobId) throws RemoteException, CredentialException {
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(proxyValue));
		
		if(dbGridJob != null) {
			GridJobInfo jobInfo = new GridJobInfo();
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(dbGridJob.getNativeJobId(), proxyValue);
			jobInfo.setJobId(dbGridJob.getJobId());
			jobInfo.setNativeJobId(dbGridJob.getNativeJobId());
			jobInfo.setStatus(gridJobStatus.getStatus());
			
			return jobInfo;
		} else {
			return null;
		}
	}

	public String getJobDescription(GridWorkerService gridWorkerService, String proxyValue, String jobId) throws CredentialException {
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(proxyValue));
		
		if(dbGridJob != null) {
			return dbGridJob.getJdl();
		}
		
		return null;
	}

	public boolean abortJob(GridWorkerService gridWorkerService, String proxyValue, String jobId) throws CredentialException, RemoteException {
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(proxyValue));
		
		if(dbGridJob != null) {
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(dbGridJob.getNativeJobId(), proxyValue);
			
			if(!asList("FAILED", "DONE", "FINISHED", "CANCELED").contains(gridJobStatus.getStatus())) {
				gridWorkerService.abortGridJob(dbGridJob.getNativeJobId(), proxyValue);
			}
			
			return true;
		} else {
			return false;
		}
	}

	public boolean deleteJob(GridWorkerService gridWorkerService, String proxyValue, String jobId) throws CredentialException, RemoteException {
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(proxyValue));
		
		if(dbGridJob != null) {
			GridJobStatus gridJobStatus = gridWorkerService.getGridJobStatus(dbGridJob.getNativeJobId(), proxyValue);
			
			if(!asList("FAILED", "DONE", "FINISHED", "CANCELED").contains(gridJobStatus.getStatus())) {
				gridWorkerService.abortGridJob(dbGridJob.getNativeJobId(), proxyValue);
			}

			gridJobRepository.delete(dbGridJob.getId());
			
			return true;
		} else {
			return false;
		}
	}

	public boolean isGridJobOwned(String jobId, String proxyValue) throws CredentialException {
		pl.cyfronet.rimrock.domain.GridJob dbGridJob = gridJobRepository.findOneByJobIdAndUserLogin(jobId, proxyHelper.getUserLogin(proxyValue));
		
		return dbGridJob != null;
	}

	public Long getFile(String proxyValue, String jobId, String fileName, OutputStream pos) throws ServerException, IOException, ClientException,
			CredentialException, GSSException {
		GridFTPClient client = null;
		
		try {
			GSSCredential gsscredential = proxyHelper.getGssCredential(proxyValue);
			client = new GridFTPClient(gridFtpHost, 2811);
			client.authenticate(gsscredential);
			client.setPassive();
			client.setLocalActive();
			
			String remoteFilePath = createBaseGridFtpDirectory(proxyHelper.getUserLogin(proxyValue)) + ".rimrock/" + jobId + "/" + fileName;
			long size = client.getSize(remoteFilePath);
			client.get(remoteFilePath, new DataSinkStream(pos), null);
			
			return size;
		} finally {
			if(client != null) {
				client.close();
			}
		}
	}

	private String createBaseGridFtpDirectory(String userLogin) {
		return "/storage/" + userLogin + "/";
	}
}