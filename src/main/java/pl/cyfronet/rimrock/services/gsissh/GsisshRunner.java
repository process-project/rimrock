package pl.cyfronet.rimrock.services.gsissh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.airavata.gsi.ssh.api.authentication.GSIAuthenticationInfo;
import org.apache.airavata.gsi.ssh.jsch.ExtendedJSch;
import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ExtendedSession;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

import pl.cyfronet.rimrock.gsi.ProxyHelper;

@Service
public class GsisshRunner {
	private static final Logger log = LoggerFactory.getLogger(GsisshRunner.class);
	
	private class NormalizedOutput {
		String output;
		int exitCode;
	}
	
	@Value("${run.timeout.millis}")
	int runTimeoutMillis;
	
	@Value("${gsissh.pool.size}")
	int poolSize;
	
	@Autowired
	ProxyHelper proxyHelper;
	
	private Map<String, AtomicInteger> logins;
	
	public GsisshRunner() {
		logins = new HashMap<>();
		initialize();
	}
	
	/**
	 * Runs the given command on the host. Authentication uses the given proxy. If given timeout
	 * (provided in millis) is greater than 0 it is used, otherwise a default value is used. 
	 */
	public RunResults run(String host, String proxyValue, String command, int timeoutMillis)
			throws JSchException, CredentialException, InterruptedException, GSSException,
			IOException {
		String userLogin = proxyHelper.getUserLogin(proxyValue);
		
		try {
			checkPool(userLogin);
			proxyHelper.verify(proxyValue);
			
			GSSCredential gsscredential = proxyHelper.getGssCredential(proxyValue);			
			JSch jsch = new ExtendedJSch();
	        ExtendedSession session = (ExtendedSession) jsch.getSession(userLogin, host);
	        session.setAuthenticationInfo(new GSIAuthenticationInfo() {
				@Override
				public GSSCredential getCredentials() throws SecurityException {
						return gsscredential;
				}
			});
	        
	        Properties config = new Properties();
	        config.put("StrictHostKeyChecking", "no");
	        session.setConfig(config);
			
	        Channel channel = null;
			RunResults results = new RunResults();
			
			try {
				session.connect();
				
				String separator = UUID.randomUUID().toString();
				channel = session.openChannel("exec");
				
		        byte[] completedCommand = completeCommand(command, separator);
		        log.trace("Running command via gsi-ssh: {}", new String(completedCommand));
				((ChannelExec) channel).setCommand(completedCommand);
		        channel.setInputStream(null);
		        ((ChannelExec) channel).setErrStream(System.err);
		        InputStream outputStream = channel.getInputStream();
		        InputStream errorStream = ((ChannelExec) channel).getErrStream();
		        channel.connect(runTimeoutMillis);
		        
		        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
		        ByteStreams.copy(outputStream, standardOutput);
		        
		        ByteArrayOutputStream standardError = new ByteArrayOutputStream();
		        ByteStreams.copy(errorStream, standardError);
				
		        String retrievedStandardOutput = new String(standardOutput.toByteArray());
		        NormalizedOutput normalizedOutput = normalizeStandardOutput(retrievedStandardOutput, separator);
		        results.setOutput(normalizedOutput.output);
		        
		        if (!results.isTimeoutOccured()) {
		        	results.setExitCode(normalizedOutput.exitCode);
		        }
		        
		        results.setError(new String(standardError.toByteArray()));
			} finally {
				synchronized(jsch) {
					if(session.isConnected()) {
						session.disconnect();
					}
					
					if (channel != null && channel.isConnected()) {
						channel.disconnect();
					}
				}
			}
			
			return results;
		} finally {
			freePool(userLogin);
		}
	}

	private void checkPool(String userLogin) throws InterruptedException {
		synchronized(logins) {
			if(!logins.containsKey(userLogin)) {
				logins.put(userLogin, new AtomicInteger(0));
			}
			
			while(logins.get(userLogin).get() >= poolSize) {
				log.debug("Thread {} awaits for gsissh execution", Thread.currentThread().getId());
				logins.wait();
			}
			
			log.debug("Thread {} granted gsissh execution", Thread.currentThread().getId());
			logins.get(userLogin).incrementAndGet();
		}
	}
	
	private void freePool(String userLogin) {
		synchronized(logins) {
			log.debug("Thread {} frees gsissh execution", Thread.currentThread().getId());
			
			int size = logins.get(userLogin).decrementAndGet();
			
			if(size == 0) {
				logins.remove(userLogin);
			}
			
			logins.notify();
		}
	}

	private NormalizedOutput normalizeStandardOutput(String output, String separator) {
		log.trace("Output being normalized: {}", output);
		
		NormalizedOutput result = new NormalizedOutput();
		
		//matching proper output
		Pattern pattern = Pattern.compile(".*^" + separator + "$\\s+(.*)^(.*?)\\s+^" + separator + "$.*",
				Pattern.MULTILINE | Pattern.DOTALL);
		Matcher matcher = pattern.matcher(output);
		
		if(matcher.matches()) {
			result.output = matcher.group(1).replaceAll("\r\n", "\n").trim();
			
			try {
				result.exitCode = Integer.parseInt(matcher.group(2));
			} catch(NumberFormatException e) {
				log.warn("Exit code {} could not be parsed");
			}
		} else {
			//trying to match everything after the first separator (in case a timeout occurred)
			Pattern fallbackPattern = Pattern.compile(".*^" + separator + "$\\s+(.*)",
					Pattern.MULTILINE | Pattern.DOTALL);
			matcher = fallbackPattern.matcher(output);
			
			if(matcher.matches()) {
				result.output = matcher.group(1).replaceAll("\r\n", "\n").trim();
			}
		}
		
		return result;
	}

	private byte[] completeCommand(String command, String separator) {
		return ("unset HISTFILE; echo '" + separator + "'; " + command + "; echo $?; echo '" + separator + "'; exit\n").getBytes();
	}

	private void initialize() {
		JSch.setConfig("gssapi-with-mic.x509", "org.apache.airavata.gsi.ssh.GSSContextX509");
	    JSch.setConfig("userauth.gssapi-with-mic",
	    		"com.jcraft.jsch.UserAuthGSSAPIWithMICGSSCredentials");
	}
}