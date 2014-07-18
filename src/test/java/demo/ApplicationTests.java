package demo;

import java.io.IOException;

import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.junit.Test;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.GSSAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.util.InvalidStateException;

public class ApplicationTests {
	private static final String HOST = "zeus.cyfronet.pl";

	@Test
	public void contextLoads() throws IOException, InvalidStateException, InterruptedException, CredentialException, GSSException {
		SshClient ssh = new SshClient();
		SshConnectionProperties properties = new SshConnectionProperties();
		properties.setHost(HOST);
		
		X509Credential proxy = new X509Credential("/home/daniel/temp/user-proxy.pem");
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		proxy.verify();
		properties.setUserProxy(gsscredential);
		ssh.connect(properties);

		GSSAuthenticationClient pwd = new GSSAuthenticationClient(gsscredential);
		pwd.setUsername("plgharezlak");

		int result = ssh.authenticate(pwd, HOST);

		if(result == AuthenticationProtocolState.COMPLETE) {
			SessionChannelClient session = ssh.openSessionChannel();

			if(!session.requestPseudoTerminal("vt100", 80, 24, 0, 0, "")) {
				System.out.println("Failed to allocate a pseudo terminal");
			}

			if(session.startShell()) {
				IOStreamConnector input = new IOStreamConnector();
				IOStreamConnector output = new IOStreamConnector();
				IOStreamConnector error = new IOStreamConnector();
				output.setCloseOutput(false);
				input.setCloseInput(false);
				error.setCloseOutput(false);
				input.connect(System.in, session.getOutputStream());
				output.connect(session.getInputStream(), System.out);
				error.connect(session.getStderrInputStream(), System.out);
				session.getState().waitForState(ChannelState.CHANNEL_CLOSED);
			} else {
				System.out.println("Failed to start the users shell");
			}

			ssh.disconnect();
		}
	}
}