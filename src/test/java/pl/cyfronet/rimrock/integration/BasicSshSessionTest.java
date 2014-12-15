package pl.cyfronet.rimrock.integration;

import static java.lang.Thread.sleep;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.services.GsisshRunner;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.GSSAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.session.SessionChannelClient;

@Ignore("This test blocks awaits stdin. Only run manually.")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class BasicSshSessionTest {
	private static final String HOST = "zeus.cyfronet.pl";
	
	@Autowired ProxyFactory proxyFactory;
	@Autowired GsisshRunner runner;

	@Test
	public void testBasicSshSession() throws Exception {
		SshClient ssh = new SshClient();
		SshConnectionProperties properties = new SshConnectionProperties();
		properties.setHost(HOST);
		
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(proxyFactory.getProxy().getBytes()));
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		proxy.verify();
		properties.setUserProxy(gsscredential);
		ssh.connect(properties);

		GSSAuthenticationClient pwd = new GSSAuthenticationClient(gsscredential);
		pwd.setUsername(extractUsername(proxy.getSubject()));

		int result = ssh.authenticate(pwd, HOST);

		if(result == AuthenticationProtocolState.COMPLETE) {
			SessionChannelClient session = ssh.openSessionChannel();

			if(!session.requestPseudoTerminal("vt100", 160, 24, 0, 0, "")) {
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
				error.connect(session.getStderrInputStream(), System.err);
				session.getState().waitForState(ChannelState.CHANNEL_CLOSED);
			} else {
				System.out.println("Failed to start the users shell");
			}

			ssh.disconnect();
		}
	}
	
	@Test
	public void testMultipleSessions() throws InterruptedException {
		List<Thread> threads = new ArrayList<>();
		
		for(int i = 0; i < 20; i++) {
			int index = i;
			
			Thread t = new Thread(() -> {
				try {
					runner.run("zeus.cyfronet.pl", proxyFactory.getProxy(), "sleep 2; echo " + index , 20000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			threads.add(t);
			t.start();
			sleep(100);
		}
		
		for(Thread t : threads) {
			t.join();
		}
	}

	private String extractUsername(String subject) {
		Pattern pattern = Pattern.compile(".+CN=(plg.+),.+");
		Matcher matcher = pattern.matcher(subject);
		
		if(matcher.matches()) {
			return matcher.group(1);
		}
		
		throw new IllegalArgumentException("Subject " + subject + " does not carry a valid user name");
	}
}