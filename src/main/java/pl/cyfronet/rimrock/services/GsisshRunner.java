package pl.cyfronet.rimrock.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.GSSAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.util.InvalidStateException;

@Service
public class GsisshRunner {
	private static final Logger log = LoggerFactory.getLogger(GsisshRunner.class);
	
	public RunResults run(String host, String proxyValue, String command) throws CredentialException, GSSException, IOException, InvalidStateException, InterruptedException {
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(proxyValue.getBytes()));
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		proxy.verify();
		
		SshConnectionProperties properties = new SshConnectionProperties();
		properties.setHost(host);
		properties.setUserProxy(gsscredential);
		
		GSSAuthenticationClient pwd = new GSSAuthenticationClient(gsscredential);
		pwd.setUsername(extractUserName(proxy.getSubject()));
		
		SshClient ssh = new SshClient();
		ssh.connect(properties);
		
		RunResults results = new RunResults();
		int result = ssh.authenticate(pwd, host);

		if(result == AuthenticationProtocolState.COMPLETE) {
			SessionChannelClient session = ssh.openSessionChannel();

			if(!session.requestPseudoTerminal("vt100", 80, 24, 0, 0, "")) {
				throw new IOException("Failed to allocate a pseudo terminal");
			}

			if(session.startShell()) {
				IOStreamConnector input = new IOStreamConnector();
				IOStreamConnector output = new IOStreamConnector();
				IOStreamConnector error = new IOStreamConnector();
				output.setCloseOutput(false);
				input.setCloseInput(false);
				error.setCloseOutput(false);
				
				String separator = UUID.randomUUID().toString();
				input.connect(new ByteArrayInputStream(completeCommand(command, separator)), session.getOutputStream());
				
				ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
				output.connect(session.getInputStream(), standardOutput);
				
				ByteArrayOutputStream standardError = new ByteArrayOutputStream();
				error.connect(session.getStderrInputStream(), standardError);
				session.getState().waitForState(ChannelState.CHANNEL_CLOSED);
				
				String retrievedStandardOutput = new String(standardOutput.toByteArray());
				results.setOutput(normalizeStandardOutput(retrievedStandardOutput, separator));
				results.setError(new String(standardError.toByteArray()));
			} else {
				throw new IOException("Failed to start the users shell");
			}

			ssh.disconnect();
		}
		
		return results;
	}

	private String normalizeStandardOutput(String output, String separator) {
		log.trace("Output being normalized: {}" + output);
		
		String result = null;
		Pattern pattern = Pattern.compile(".*^" + separator + "$\\s+(.*?)\\s+^" + separator + "$.*",
				Pattern.MULTILINE | Pattern.DOTALL);
		Matcher matcher = pattern.matcher(output);
		
		if(matcher.matches()) {
			result = matcher.group(1);
		}
		
		return result;
	}

	private byte[] completeCommand(String command, String separator) {
		return ("echo '" + separator + "'; " + command + "; echo '" + separator + "'; exit\n").getBytes();
	}

	private String extractUserName(String subject) {
		Pattern pattern = Pattern.compile(".+CN=(plg.+),.+");
		Matcher matcher = pattern.matcher(subject);
		
		if(matcher.matches()) {
			return matcher.group(1);
		}
		
		throw new IllegalArgumentException("Subject " + subject + " does not carry a valid user name");
	}
}