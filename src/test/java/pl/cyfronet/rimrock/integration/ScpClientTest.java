package pl.cyfronet.rimrock.integration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import pl.cyfronet.rimrock.RimrockApplication;

@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class ScpClientTest {
	private static final Logger log = LoggerFactory.getLogger(ScpClientTest.class);

	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Value("${test.user.login}")
	private String userLogin;

	@Value("${test.user.password}")
	private String userPassword;

	private String host = "ui.cyfronet.pl";

	@Test
	public void testJsch() throws JSchException, SftpException {
		Instant i1 = Instant.now();
		JSch jsch = new JSch();
		Session session = jsch.getSession(userLogin, host, 22);
		session.setPassword(userPassword);
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();
		
		Channel channel = session.openChannel("sftp");
		channel.connect();
		
		ChannelSftp c = (ChannelSftp) channel;
		
		String certPath = "/mnt/keyfs/users/" + userLogin + "/.globus/usercert.pem";
		String keyPath = "/mnt/keyfs/users/" + userLogin + "/.globus/userkey.pem";
		SftpATTRS cert = c.stat(certPath);
		log.info(cert.toString());
		
		if(cert.isReg()) {
			InputStream is = c.get(certPath);
			log.info("Cert: {}", new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n")));
		}
		
		SftpATTRS key = c.stat(keyPath);
		log.info(key.toString());
		
		if(key.isReg()) {
			InputStream keyIs = c.get(keyPath);
			log.info("Key: {}", new BufferedReader(new InputStreamReader(keyIs)).lines().collect(Collectors.joining("\n")));
		}
		
		Instant i2 = Instant.now();
		log.info("Cert and key retrieval time in ms: {}", Duration.between(i1, i2).toMillis());
		
		try {
			SftpATTRS missing = c.stat("/mnt/keyfs/users/plgtesthar/.globus/missing");
			log.info(missing.toString());
		} catch (SftpException e) {
			log.info("The file is missing as it should");
		}
		
		c.exit();
	}
}