package pl.cyfronet.rimrock.integration;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
	@Value("${test.proxy.path}") 
	private String proxyPath;
	
	@Value("${test.uploadDir.path}")
	private String uploadDir;
	
	@Autowired 
	private FileManagerFactory factory;
	
	@Autowired 
	private ProxyFactory proxyFactory;
	
	@Test
	public void shouldUploadFile() throws Exception {			
		FileManager manager = factory.get(proxyFactory.getProxy());
		manager.copyFile(uploadDir, new FileSystemResource(new File("pom.xml")));
	}	
}
