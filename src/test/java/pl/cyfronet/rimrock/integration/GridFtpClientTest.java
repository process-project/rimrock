package pl.cyfronet.rimrock.integration;

import java.io.ByteArrayInputStream;

import org.globus.ftp.FileInfo;
import org.globus.ftp.GridFTPClient;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;

@Ignore("This may come in handy in the future...")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class GridFtpClientTest {
	private final static Logger log = LoggerFactory.getLogger(GridFtpClientTest.class);
	
	@Autowired private ProxyFactory proxyFactory;
	
	@Test
	public void shouldCopyFile() throws Exception {
		GridFTPClient client = new GridFTPClient("reef.man.poznan.pl", 2811);
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(
				proxyFactory.getProxy().getBytes()));
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		client.authenticate(gsscredential);
		client.setPassive();
		client.setLocalActive();
		
		if(!client.exists(".rimrock")) {
			client.makeDir(".rimrock");
		}
		
		log.info("Current folder: {}", client.getCurrentDir());
		
		for(Object o : client.list()) {
			FileInfo fileInfo = (FileInfo) o;
			log.info("File {} of type {}", fileInfo.getName(), fileInfo.isDirectory() ? "directory" : "file");
		}
	}
}