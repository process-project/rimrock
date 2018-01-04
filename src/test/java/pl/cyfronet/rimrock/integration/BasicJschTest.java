package pl.cyfronet.rimrock.integration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import org.apache.airavata.gsi.ssh.api.authentication.GSIAuthenticationInfo;
import org.apache.airavata.gsi.ssh.jsch.ExtendedJSch;
import org.ietf.jgss.GSSCredential;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.ByteStreams;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ExtendedSession;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class BasicJschTest {

    @Value("${test.user.login}")
    private String userLogin;

	@Autowired
	ProxyFactory proxyFactory;

	@Autowired
	private ProxyHelper proxyHelper;

	@Test
	public void testJsch() throws JSchException, IOException {
		JSch.setConfig("gssapi-with-mic.x509", "org.apache.airavata.gsi.ssh.GSSContextX509");
        JSch.setConfig("userauth.gssapi-with-mic",
        		"com.jcraft.jsch.UserAuthGSSAPIWithMICGSSCredentials");

        JSch jsch = new ExtendedJSch();

        ExtendedSession session = (ExtendedSession) jsch.getSession(userLogin,
        		"zeus.cyfronet.pl", 22);
        session.setAuthenticationInfo(new GSIAuthenticationInfo() {
			@Override
			public GSSCredential getCredentials() throws SecurityException {
				try {
					Instant start = Instant.now();
					GSSCredential gssCredential = proxyHelper.getGssCredential(
							proxyFactory.getProxy());
					System.out.println("Proxy generation time: " + Duration.between(start,
							Instant.now()).toMillis());

					return gssCredential;
				} catch (Exception e) {
					return null;
				}
			}
		});

        Instant start = Instant.now();
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand("echo hello");
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        channel.connect();

        ByteStreams.copy(channel.getInputStream(), System.out);
        channel.disconnect();
        session.disconnect();

        System.out.println("Time: " + Duration.between(start, Instant.now()).toMillis());
	}
}