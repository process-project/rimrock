package pl.cyfronet.rimrock.integration;

import java.io.ByteArrayInputStream;
import java.io.File;
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
	@Ignore
	public void shouldUploadFile() throws Exception {
		String uploadPath = getHomedir() + "/a/b/c/upload_test.xml";
		getFileManager().cp(uploadPath, new FileSystemResource(new File("pom.xml")));
	}

	@Test
	@Ignore
	public void shouldRmFile() throws Exception {
		String path = getHomedir() + "/upload_test.xml";
		getFileManager().rm(path);
	}

	@Test
	@Ignore
	public void shouldRmDir() throws Exception {
		String path = getHomedir() + "/delete_test";
		getFileManager().rmDir(path);
	}

	private String getHomedir() throws Exception {
		return "/people/" + getUser();
	}

	private String getUser() throws Exception {
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(proxyFactory.getProxy().getBytes()));
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

	private FileManager getFileManager() throws Exception {
		String proxy = proxyFactory.getProxy();
		return factory.get(proxy);
	}
}
