package pl.cyfronet.rimrock.integration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.globus.ftp.FileInfo;
import org.globus.ftp.GridFTPClient;
import org.globus.ftp.exception.ClientException;
import org.globus.ftp.exception.ServerException;
import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.RimrockApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class GridFtpClientTest {
	private final static Logger log = LoggerFactory.getLogger(GridFtpClientTest.class);
	
	@Value("${test.proxy.path}") String proxyPath;
	
	@Test
	public void shouldCopyFile() throws ServerException, IOException, GSSException, CredentialException, ClientException {
		GridFTPClient client = new GridFTPClient("zeus.cyfronet.pl", 2811);
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(
				new String(Files.readAllBytes(Paths.get(proxyPath))).getBytes()));
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		client.authenticate(gsscredential);
		client.setPassive();
		client.setLocalActive();
		log.info("Current folder: {}", client.getCurrentDir());
		
		for(Object o : client.list()) {
			FileInfo fileInfo = (FileInfo) o;
			log.info("File {} of type {}", fileInfo.getName(), fileInfo.isDirectory() ? "directory" : "file");
		}
	}
}