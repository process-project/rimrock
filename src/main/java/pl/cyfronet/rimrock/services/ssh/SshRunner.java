package pl.cyfronet.rimrock.services.ssh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Service
public class SshRunner extends SshRunnerBase {
	/**
	 * Runs the given command on the host. Authentication uses the given proxy. If given timeout
	 * (provided in millis) is greater than 0 it is used, otherwise a default value is used.
	 */
	public RunResults run(String host, String proxyValue, String command, String workingDirectory,
			int timeoutMillis, String sshUser, String sshPrivKey)
			throws JSchException, CredentialException, InterruptedException, GSSException,
			IOException {
		String userLogin = proxyHelper.getUserLogin(proxyValue);

		try {
			checkPool(userLogin);
			proxyHelper.verify(proxyValue);

			JSch jsch = new JSch();
			Session session;
			
			jsch.addIdentity(sshUser, decodeKey(sshPrivKey).getBytes(), null, null);
	        session = jsch.getSession(sshUser, host);

	        Properties config = new Properties();
	        config.put("StrictHostKeyChecking", "no");
	        session.setConfig(config);

			RunResults results = new RunResults();
			String separator = UUID.randomUUID().toString();

			try {
				session.connect();

				Channel channel = session.openChannel("exec");

		        byte[] completedCommand = completeCommand(command, separator, workingDirectory);
		        log.trace("Running command via ssh: {}", new String(completedCommand));
				((ChannelExec) channel).setCommand(completedCommand);
		        channel.setInputStream(null);
		        ((ChannelExec) channel).setErrStream(System.err);
		        InputStream outputStream = channel.getInputStream();
		        InputStream errorStream = ((ChannelExec) channel).getErrStream();
		        channel.connect();

		        Thread timeoutThread = new Thread() {
					@Override
					public void run() {
						try {
							Thread.sleep(timeoutMillis > 0 ? timeoutMillis : runTimeoutMillis);
						} catch (InterruptedException e) {
							return;
						}

						log.debug("Timeout thread awoke. Setting timeout state...");

						synchronized(jsch) {
							results.setTimeoutOccured(true);

							if(session.isConnected()) {
								session.disconnect(); //this also disconnects all channels
							}
						}
					};
				};
				timeoutThread.start();


		        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
		        ByteStreams.copy(outputStream, standardOutput);

		        ByteArrayOutputStream standardError = new ByteArrayOutputStream();
		        ByteStreams.copy(errorStream, standardError);

		        if(timeoutThread.isAlive()) {
		        	timeoutThread.interrupt();
		        }

		        String retrievedStandardOutput = new String(standardOutput.toByteArray());
		        NormalizedOutput normalizedOutput = normalizeStandardOutput(retrievedStandardOutput,
		        		separator);
		        results.setOutput(normalizedOutput.output);

		        if (!results.isTimeoutOccured()) {
		        	results.setExitCode(normalizedOutput.exitCode);
		        }

		        results.setError(new String(standardError.toByteArray()));
			} finally {
				synchronized(jsch) {
					if(session.isConnected()) {
						session.disconnect(); //this also disconnects all channels
					}
				}
			}

			return results;
		} finally {
			freePool(userLogin);
		}
	}

	protected void initialize() {}
	
	private String decodeKey(String key) {
		return new String(Base64.getDecoder().decode(key), Charset.forName("utf-8"));
	}
}
