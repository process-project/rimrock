package pl.cyfronet.rimrock.integration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.services.filemanager.FileManager;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class FileUploadTest {
	@Autowired private FileManagerFactory factory;
	@Autowired private ProxyFactory proxyFactory;
	
	@Test
	public void shouldUploadFile() throws Exception {			
		String proxy = proxyFactory.getProxy();
		String uploadPath = "/people/" + getUser(proxy) + "/upload_test.xml";
		FileManager manager = factory.get(proxy);
		manager.copyFile(uploadPath, new FileSystemResource(new File("pom.xml")));
	}

	private String getUser(String proxyValue) throws CredentialException, GSSException {
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(proxyValue.getBytes()));
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		String dn = gsscredential.getName().toString();
		Pattern pattern = Pattern.compile(".*=(.*)$");
		Matcher matcher = pattern.matcher(dn);
		
		if(matcher.matches()) {
			return matcher.group(1);
		} else {
			throw new IllegalArgumentException("Could not extract user name from the supplied user proxy");
		}
	}
}
