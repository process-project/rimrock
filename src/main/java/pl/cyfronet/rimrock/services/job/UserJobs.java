package pl.cyfronet.rimrock.services.job;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import pl.cyfronet.rimrock.controllers.rest.PathHelper;
import pl.cyfronet.rimrock.controllers.rest.jobs.JobNotFoundException;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;
import pl.cyfronet.rimrock.services.GsisshRunner;
import pl.cyfronet.rimrock.services.RunException;
import pl.cyfronet.rimrock.services.RunResults;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sshtools.j2ssh.util.InvalidStateException;

public class UserJobs {
	private static final Logger log = LoggerFactory.getLogger(UserJobs.class);

	private int timeout = 60000;
	private String proxy;
	private String userLogin;
	private GsisshRunner runner;
	private JobRepository jobRepository;
	private ObjectMapper mapper;
	private FileManager fileManager;

	public UserJobs(String proxy, FileManagerFactory fileManagerFactory,
			GsisshRunner runner, JobRepository jobRepository,
			ProxyHelper proxyHelper, ObjectMapper mapper)
			throws CredentialException, GSSException, KeyStoreException, CertificateException, IOException {
		this.proxy = proxy;
		this.userLogin = proxyHelper.getUserLogin(proxy);
		this.runner = runner;
		this.jobRepository = jobRepository;
		this.mapper = mapper;
		this.fileManager = fileManagerFactory.get(proxy);
	}

	/**
	 * Submit new job into queues system.
	 * 
	 * @param host Host where job will be submitted
	 * @param workingDirectory Job working directory.
	 * @param script Job script payload.
	 * 
	 * @return Information about job status, std oud and std err file paths.
	 * 
	 * @throws FileManagerException Thrown when there is not possible to transfer start job scripts.
	 * @throws CredentialException Thrown where it is not possible to log in into given host using user credentials.
	 * @throws RunException Thrown when any error connected with executing job submissions on given host occurs.
	 * @throws CertificateException 
	 * @throws KeyStoreException 
	 */
	public Job submit(String host, String workingDirectory, String script) throws FileManagerException, CredentialException, RunException, KeyStoreException, CertificateException {
		String rootPath = buildRootPath(host, workingDirectory, proxy);
		log.debug("Starting {} user job in {}:{} ", new Object[] {userLogin, host, rootPath});

		fileManager.cp(rootPath + "script.sh", new ByteArrayResource(script.getBytes()));
		fileManager.cp(rootPath + "start", new ClassPathResource("scripts/start"));

		RunResults result = run(host, String.format("cd %s; chmod +x start; ./start script.sh", rootPath), timeout);
		processRunExceptions(result);

		SubmitResult submitResult = readResult(result.getOutput(), SubmitResult.class);
		
		if ("OK".equals(submitResult.getResult())) {
			return jobRepository.save(new Job(submitResult.getJobId(), "QUEUED", submitResult.getStandardOutputLocation(),
					submitResult.getStandardErrorLocation(), userLogin, host));
		} else {
			throw new RunException(submitResult.getErrorMessage(), result);
		}
	}

	/**
	 * Update job statuses started on selected hosts.
	 * 
	 * @param hosts Jobs started on these hosts will be updated.
	 * 
	 * @return Updated jobs.
	 * 
	 * @throws CredentialException Thrown where it is not possible to log in into given host using user credentials.
	 * @throws FileManagerException Thrown when there is not possible to transfer start job scripts.
	 * @throws CertificateException 
	 * @throws KeyStoreException 
	 * @throws RunException 
	 */
	public List<Job> update(List<String> hosts) throws CredentialException,
			FileManagerException, RunException, KeyStoreException, CertificateException {
		if (hosts == null || hosts.size() == 0) {
			return Arrays.asList();
		}

		List<Status> statuses = new ArrayList<Status>();
		
		for (String host : hosts) {
			StatusResult statusResult = getStatusResult(host);

			if (statusResult.getErrorMessage() != null) {
				throw new RunException(statusResult.getErrorMessage());
			}

			statuses.addAll(statusResult.getStatuses());
		}

		List<Job> jobs = jobRepository.findByUserOnHosts(userLogin, hosts);
		Map<String, Status> mappedStatusJobIds = statuses.stream()
				.collect(Collectors.toMap(Status::getJobId, Function.<Status> identity()));

		for (Job job : jobs) {
			Status status = mappedStatusJobIds.get(job.getJobId());
			
			if(!Arrays.asList("FINISHED", "ABORTED").contains(job.getStatus())) {
				job.setStatus(status != null ? status.getStatus() : "FINISHED");
				jobRepository.save(job);
			}
		}

		return jobs;
	}

	/**
	 * Delete job. If job is in state different then "FINISHED", than it is also
	 * deleted from the infrastructure.
	 * 
	 * @param jobId Job identifier.
	 * 
	 * @throws JobNotFoundException Thrown when job is not found in the database.
	 * @throws CredentialException Thrown where it is not possible to log in into given host using user credentials.
	 * @throws FileManagerException Thrown when there is not possible to transfer start job scripts.
	 * @throws CertificateException 
	 * @throws KeyStoreException 
	 * @throws RunException 
	 */
	public void delete(String jobId) throws JobNotFoundException, CredentialException, FileManagerException, RunException, KeyStoreException, CertificateException {
		Job job = abortJob(jobId);
		jobRepository.delete(job);
	}
	
	public void abort(String jobId) throws CredentialException, RunException, FileManagerException, JobNotFoundException, KeyStoreException, CertificateException {
		Job job = abortJob(jobId);
		job.setStatus("ABORTED");
		jobRepository.save(job);
	}

	private Job abortJob(String jobId) throws JobNotFoundException, FileManagerException, CredentialException, RunException, KeyStoreException, CertificateException {
		Job job = jobRepository.findOneByJobId(jobId);

		if(job == null) {
			throw new JobNotFoundException(jobId);
		}

		if(!"FINISHED".equals(job.getStatus())) {
			String host = job.getHost();
			String rootPath = PathHelper.getRootPath(host, userLogin);
			fileManager.cp(rootPath + ".rimrock/stop", new ClassPathResource("scripts/stop"));

			RunResults result = run(host, String.format("cd %s.rimrock; chmod +x stop; ./stop %s", rootPath, jobId), timeout);
			processRunExceptions(result);
		}
		return job;
	}

	public Job get(String jobId) {
		return jobRepository.findOneByJobIdAndUser(jobId, userLogin);		
	}
	
	private <T> T readResult(String output, Class<T> klass) {
		try {
			return mapper.readValue(output, klass);
		} catch (Exception e) {
			throw new RunException(e.getMessage());
		}
	}

	private StatusResult getStatusResult(String host)
			throws CredentialException, RunException, FileManagerException, KeyStoreException, CertificateException {
		String rootPath = PathHelper.getRootPath(host, userLogin);
		fileManager.cp(rootPath + ".rimrock/status", new ClassPathResource(
				"scripts/status"));
		RunResults result = run(host, String.format(
				"cd %s.rimrock; chmod +x status; ./status", rootPath), timeout);

		if (result.isTimeoutOccured() || result.getExitCode() != 0) {
			StatusResult statusResult = new StatusResult();
			statusResult.setResult("ERROR");
			statusResult.setErrorMessage(result.getError());

			return statusResult;
		} else {
			return readResult(result.getOutput(), StatusResult.class);
		}
	}

	private RunResults run(String host, String command, int timeout)
			throws CredentialException, RunException, KeyStoreException, CertificateException {
		try {
			return runner.run(host, proxy, command, timeout);
		} catch (InvalidStateException | GSSException | IOException
				| InterruptedException e) {
			throw new RunException(e.getMessage());
		}
	}

	private String buildRootPath(String host, String workingDirectory,
			String proxy) throws CredentialException {
		String rootPath = workingDirectory == null ? PathHelper.getRootPath(host, userLogin)
				: workingDirectory;

		if (!rootPath.endsWith("/")) {
			rootPath = rootPath + "/";
		}

		return rootPath;
	}

	private void processRunExceptions(RunResults result) {
		if (result.isTimeoutOccured()) {
			throw new RunException(
					"Unable to submit new job because of timeout", result);
		}

		if (result.getExitCode() != 0) {
			throw new RunException("Submission of new job failed", result);
		}
	}
}