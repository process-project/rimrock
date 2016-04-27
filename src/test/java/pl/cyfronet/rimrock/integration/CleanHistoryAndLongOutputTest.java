package pl.cyfronet.rimrock.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.services.gsissh.GsisshRunner;
import pl.cyfronet.rimrock.services.gsissh.RunResults;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@DirtiesContext
public class CleanHistoryAndLongOutputTest {
	private static final Logger log = LoggerFactory.getLogger(CleanHistoryAndLongOutputTest.class);
	
	@Autowired private ProxyFactory proxyFactory;
	@Autowired private GsisshRunner runner;
	
	@Value("${run.timeout.millis}") private int runTimeoutMillis;
	
	@Test
	public void testWhetherHistoryIsClean() throws CredentialException, KeyStoreException,
			CertificateException, GSSException, IOException,
			InterruptedException, Exception {
		RunResults history = runner.run("ui.cyfronet.pl", proxyFactory.getProxy(),
				"cat .bash_history | sha1sum", runTimeoutMillis);
		log.info("History: {}", history.getOutput());
		runner.run("ui.cyfronet.pl", proxyFactory.getProxy(), "echo hello", runTimeoutMillis);
		
		RunResults newHistory = runner.run("ui.cyfronet.pl", proxyFactory.getProxy(),
				"cat .bash_history | sha1sum", runTimeoutMillis);
		log.info("New history: {}", newHistory.getOutput());
		assertEquals(history.getOutput(), newHistory.getOutput());
	}
	
	@Test
	public void testLongOutput() throws CredentialException, KeyStoreException,
			CertificateException, GSSException, IOException,
			InterruptedException, Exception {
		long outputLength = 5120;
		RunResults result = runner.run("ui.cyfronet.pl", proxyFactory.getProxy(),
				"cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w " + outputLength + " | head -n 1",
				runTimeoutMillis);
		assertEquals(outputLength, result.getOutput().length());
	}
}