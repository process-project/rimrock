package pl.cyfronet.rimrock.services.job;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;

import pl.cyfronet.rimrock.controllers.rest.PathHelper;
import pl.cyfronet.rimrock.controllers.rest.jobs.JobNotFoundException;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerException;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;
import pl.cyfronet.rimrock.services.ssh.GsisshRunner;
import pl.cyfronet.rimrock.services.ssh.RunException;
import pl.cyfronet.rimrock.services.ssh.RunResults;

public class UserJobs {
	private static final Logger log = LoggerFactory.getLogger(UserJobs.class);
	
	// TODO: Mock - remove when not needed
		private static final int LRZ_MOCK_ID = 12345678;
	// --------------------

	private int timeout = 20000;
	private String proxy;
	private String userLogin;
	private GsisshRunner runner;
	private JobRepository jobRepository;
	private ObjectMapper mapper;
	private FileManager fileManager;

	public UserJobs(String proxy, FileManagerFactory fileManagerFactory,
					GsisshRunner runner, JobRepository jobRepository,
					ProxyHelper proxyHelper, ObjectMapper mapper)
			throws CredentialException, GSSException, KeyStoreException, CertificateException,
			IOException {

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
	 * @param host             Host where job will be submitted
	 * @param workingDirectory Job working directory.
	 * @param script           Job script payload.
	 * @return Information about job status, std oud and std err file paths.
	 */
	public Job submit(String host, String workingDirectory, String script, String tag)
			throws FileManagerException, CredentialException, RunException, KeyStoreException,
			CertificateException, JSchException {

		PathHelper pathHelper = new PathHelper(host, userLogin);
		String transferPath = buildPath(pathHelper.getTransferPath(),
				pathHelper.addHostPrefix(workingDirectory));
		String fileRootPath = buildPath(pathHelper.getFileRootPath(), workingDirectory);
		log.info("Starting {} user job in {}:{} ", new Object[] { userLogin, host, transferPath });

		String scriptFileName = "script-" + UUID.randomUUID().toString() + ".sh";
		
		SubmitResult submitResult = null;
		RunResults result = null;
		
		if ( "lxlogin5.lrz.de".equals(host)) {
			// TODO: Mock - remove when not needed
			log.info("MOCK LRZ");
			
			String full_fake_job_id = LRZ_MOCK_ID + ".lxlogin5.lrz.de";
			
			List<Job> jl = jobRepository.findByHost(host);
			int jl_size = jl.size();
			
			if (jl_size > 0) {
				Job last_lrz_job = jl.get(jl.size()-1);
				String last_lrz_job_id = last_lrz_job.getJobId();
				String unscoped_lji = (last_lrz_job_id.split("\\.", 2))[0];
				int fake_job_id = Integer.parseInt(unscoped_lji) + 1;
				full_fake_job_id = fake_job_id + ".lxlogin5.lrz.de";
				
				log.info("LAST: {}, ID: {}", last_lrz_job, last_lrz_job_id);
			} else {	
				log.info("NO LAST");
			}
			
			result = new RunResults();
			result.setExitCode(0);
			result.setError("");
			result.setOutput("");
			
			String res_json = "{\n\t\"result\": \"OK\",\n\t\"job_id\": \""+
			full_fake_job_id +
			"\",\n\t\"standard_output\": \"out\",\n\t\"standard_error\": \"err\"\n}";
			submitResult = readResult(res_json, SubmitResult.class);
			log.info("RES: {} , JOB_ID: {}, OUT: {}, ERR: {}",submitResult.getResult(), submitResult.getJobId(), 
					submitResult.getStandardOutputLocation(), submitResult.getStandardErrorLocation());
			// ------------------------------------------------
		} else {
			log.info("REAL");
			
			fileManager.cp(transferPath + scriptFileName, new ByteArrayResource(script.getBytes()));
			fileManager.cp(transferPath + ".rimrock/start", new ClassPathResource("scripts/start"));
	
			result = run(host,
					String.format("cd %s; chmod +x .rimrock/start; ./.rimrock/start " + scriptFileName,
							fileRootPath), timeout);
			processRunExceptions(result);
	
			submitResult = readResult(result.getOutput(), SubmitResult.class);
		}
		
		if ("OK".equals(submitResult.getResult())) {
			String jobStatus = "QUEUED";
			log.info("Local job {} sbumitted and saved with status {}",
					submitResult.getJobId(), jobStatus);
			return jobRepository.save(new Job(submitResult.getJobId(), jobStatus,
					submitResult.getStandardOutputLocation(),
					submitResult.getStandardErrorLocation(), userLogin, host, tag));
		} else {
			throw new RunException(submitResult.getErrorMessage(), result);
		}
	}

	/**
	 * Update job statuses started on selected hosts.
	 *
	 * @param hosts Limit hosts to the supplied ones. If null is given all hosts for a given user will be used based on job history.
	 * @param tag
	 * @return Updated jobs.
	 */
	public List<Job> update(List<String> hosts, String tag, List<String> overrideJobIds)
			throws CredentialException, FileManagerException, RunException, KeyStoreException,
			CertificateException, JSchException {

		if (hosts == null) {
			hosts = jobRepository.getHosts(userLogin);
		}

		if (hosts.size() == 0) {
			return Arrays.asList();
		}

		List<Status> statuses = new ArrayList<>();
		List<History> histories = new ArrayList<>();

		for (String host : hosts) {
			List<String> jobIds = jobRepository.getNotTerminalJobIdsForUserLoginAndHost(userLogin,
					host);

			if (overrideJobIds != null) {
				jobIds.retainAll(overrideJobIds);
			}

			if (jobIds.size() > 0) {
				StatusResult statusResult = getStatusResult(host, jobIds);

				if (statusResult.getErrorMessage() != null) {
					throw new RunException(statusResult.getErrorMessage());
				}

				statuses.addAll(statusResult.getStatuses());
				histories.addAll(statusResult.getHistory());
			}
		}

		List<Job> jobs = null;

		if (tag != null) {
			jobs = jobRepository.findByUsernameAndTagOnHosts(userLogin, tag, hosts);
		} else {
			jobs = jobRepository.findByUsernameOnHosts(userLogin, hosts);
		}

		if (overrideJobIds != null) {
			jobs = jobs.stream().filter(job -> overrideJobIds.contains(job.getJobId()))
						.collect(Collectors.toList());
		}

		//toMap uses a BinaryOperator as the third parameter to deal with status duplicates
		Map<String, Status> mappedStatusJobIds = statuses.stream().collect(
				Collectors.toMap(
						Status::getJobId,
						Function.<Status>identity(),
						(a, b) -> a
				));

		//toMap uses a BinaryOperator as the third parameter to deal with history duplicates
		Map<String, History> mappedHistoryJobIds = histories.stream().collect(
				Collectors.toMap(
						History::getJobId,
						Function.<History>identity(),
						(a, b) -> a
				));

		for (Job job : jobs) {
			String status = mappedStatusJobIds.get(job.getJobId()) != null
					&& mappedStatusJobIds.get(job.getJobId()).getStatus() != null
					? mappedStatusJobIds.get(job.getJobId()).getStatus()
							: "FINISHED";
			History history = mappedHistoryJobIds.get(job.getJobId());

			//changing job status only if new state is different than the old one
			if (!job.getStatus().equals(status)) {
				if (asList("FINISHED", "ABORTED").contains(job.getStatus())) {
					log.warn("Local job {} with a terminal state ({}) attempt to change to {} "
							+ "prevented", job.getJobId(), job.getStatus(), status);
				} else {
					log.info("Local job {} changed status from {} to {}",
							job.getJobId(), job.getStatus(), status);
					job.setStatus(status);
					jobRepository.save(job);
				}
			}

			if (Arrays.asList("FINISHED", "ABORTED").contains(job.getStatus())
					&& job.getCores() == null && history != null) {
				job.setNodes(history.getJobNodes());
				job.setCores(history.getJobCores());
				job.setWallTime(history.getJobWalltime());
				job.setQueueTime(history.getJobQueuetime());
				job.setStartTime(history.getJobStarttime());
				job.setEndTime(history.getJobEndtime());
				jobRepository.save(job);
			}

		}

		return jobs;
	}

	/**
	 * Delete job. If job is in state different then "FINISHED", than it is also
	 * deleted from the infrastructure.
	 */
	public void delete(String jobId) throws JobNotFoundException, CredentialException,
			FileManagerException, RunException, KeyStoreException,
			CertificateException, JSchException {

		Job job = abortJob(jobId);
		log.info("Local job {} deleted", job.getJobId());
		jobRepository.delete(job);
	}

	public void abort(String jobId) throws CredentialException, RunException, FileManagerException,
			JobNotFoundException, KeyStoreException, CertificateException, JSchException {

		Job job = abortJob(jobId);
		job.setStatus("ABORTED");
		log.info("Local job {} aborted", job.getJobId());
		jobRepository.save(job);
	}

	public Job get(String jobId) {
		return jobRepository.findOneByJobIdAndUserLogin(jobId, userLogin);
	}

	private Job abortJob(String jobId) throws JobNotFoundException, FileManagerException,
			CredentialException, RunException, KeyStoreException,
			CertificateException, JSchException {

		Job job = jobRepository.findOneByJobId(jobId);

		if (job == null) {
			throw new JobNotFoundException(jobId);
		}

		if (!"FINISHED".equals(job.getStatus())) {
			String host = job.getHost();
			PathHelper pathHelper = new PathHelper(host, userLogin);
			fileManager.cp(pathHelper.getTransferPath() + ".rimrock/stop",
					new ClassPathResource("scripts/stop"));

			RunResults result = run(host, String.format("cd %s.rimrock; chmod +x stop; ./stop %s",
					pathHelper.getFileRootPath(), jobId), timeout);
			processRunExceptions(result);
			log.info("Local job {} aborted on the computing infrastructure as it was not "
					+ "completed yet", jobId);
		}

		return job;
	}

	private <T> T readResult(String output, Class<T> klass) {
		try {
			return mapper.readValue(output, klass);
		} catch (Exception e) {
			throw new RunException(e.getMessage());
		}
	}

	private StatusResult getStatusResult(String host, List<String> jobIds)
			throws CredentialException, RunException, FileManagerException, KeyStoreException,
			CertificateException, JSchException {
		
		if ("lxlogin5.lrz.de".equals(host)) {
			// FIXME: LRZ MOCK
//	        String res = "{\n" +
//	                "        \"statuses\": [\n" +
//	                "\n" +
//	                "        ],\n" +
//	                "\t\"history\": [\n" +
//	                "                {\n" +
//	                "                        \"job_id\": \"16793253\",\n" +
//	                "                        \"job_nodes\": \"1\",\n" +
//	                "                        \"job_cores\": \"1\",\n" +
//	                "                        \"job_walltime\": \"00:05:02\",\n" +
//	                "                        \"job_queuetime\": \"2019-10-10T15:00:12\",\n" +
//	                "                        \"job_starttime\": \"2019-10-10T15:00:19\",\n" +
//	                "                        \"job_endtime\": \"2019-10-10T15:05:21\"\n" +
//	                "                },\n" +
//	                "                {\n" +
//	                "                        \"job_id\": \"16793290\",\n" +
//	                "                        \"job_nodes\": \"1\",\n" +
//	                "                        \"job_cores\": \"1\",\n" +
//	                "                        \"job_walltime\": \"00:05:02\",\n" +
//	                "                        \"job_queuetime\": \"2019-10-10T15:12:33\",\n" +
//	                "                        \"job_starttime\": \"2019-10-10T15:14:36\",\n" +
//	                "                        \"job_endtime\": \"2019-10-10T15:19:38\"\n" +
//	                "                }\n" +
//	                "        ],\n" +
//	                "\t\"result\": \"OK\"\n" +
//	                "}\n";
//			

			String res = "{\n" +
	                "\t\"statuses\": [\n" +
	                "\t\t{\n" +
	                "\t\t\t\"job_id\": \"16793253\",\n" +
	                "\t\t\t\"job_state\": \"RUNNING\"\n" +
	                "\t\t}\n" +
	                "\t],\n" +
	                "\t\"history\": [\n" +
	                "\n" +
	                "\t],\n" +
	                "\t\"result\": \"OK\"\n" +
	                "}\n";
		

			
			return readResult(res, StatusResult.class);
			
//			StatusResult statusResult = new StatusResult();
//			statusResult.setResult("ERROR");
//			statusResult.setErrorMessage("Not yet implemented");
//			return statusResult;
		} else {
			PathHelper pathHelper = new PathHelper(host, userLogin);
			fileManager.cp(pathHelper.getTransferPath() + ".rimrock/status",
					new ClassPathResource("scripts/status"));
			RunResults result = run(host, String.format("cd %s.rimrock; chmod +x status; ./status %s",
					pathHelper.getFileRootPath(), jobIds.stream().collect(Collectors.joining(" "))),
					timeout);
	
			if (result.isTimeoutOccured() || result.getExitCode() != 0) {
				StatusResult statusResult = new StatusResult();
				statusResult.setResult("ERROR");
				statusResult.setErrorMessage(result.getError());
	
				if ((statusResult.getErrorMessage() == null
						|| statusResult.getErrorMessage().isEmpty())
						&& result.isTimeoutOccured()) {
					statusResult.setErrorMessage("Timeout occurred");
				}
	
				return statusResult;
			} else {
				return readResult(result.getOutput(), StatusResult.class);
			}
		}
	}

	private RunResults run(String host, String command, int timeout) throws CredentialException,
			RunException, KeyStoreException, CertificateException, JSchException {

		try {
			RunResults runResults = runner.run(host, proxy, command, null, timeout);
			log.debug("Run results for command [{}] are the following: {}", command, runResults);

			return runResults;
		} catch (GSSException | IOException	| InterruptedException e) {
			throw new RunException(e.getMessage(), e);
		}
	}

	private String buildPath(String rootPath, String workingDirectory) throws CredentialException {
		String path = workingDirectory == null ? rootPath : workingDirectory;

		if (!path.endsWith("/")) {
			path = path + "/";
		}

		return path;
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
