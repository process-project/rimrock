package pl.cyfronet.rimrock.integration;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;

import org.globus.ftp.DataSourceStream;
import org.globus.ftp.FileInfo;
import org.globus.ftp.GridFTPClient;
import org.ietf.jgss.GSSCredential;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

@Ignore("This may come in handy in the future...")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class GridFtpClientTest {
	private final static Logger log = LoggerFactory.getLogger(GridFtpClientTest.class);
	
	@Autowired private ProxyFactory proxyFactory;
	@Autowired private ProxyHelper proxyHelper;
	
	@Value("${grid.ftp.host}") private String gridFtpHost;
	
	@Test
	public void shouldCopyFile() throws Exception {
		GridFTPClient client = null;
		
		Instant start = Instant.now();
		
		try {
			GSSCredential gsscredential = proxyHelper.getGssCredential(proxyFactory.getProxy());
			client = new GridFTPClient(gridFtpHost, 2811);
			client.authenticate(gsscredential);
			client.setPassive();
			client.setLocalActive();
			
			if(!client.exists(".rimrock")) {
				client.makeDir(".rimrock");
			}
			
			String remoteDir = client.getCurrentDir();
			log.info("Current folder: {}", remoteDir);
			client.changeDir(".rimrock");
			client.put("file.txt", new DataSourceStream(new ByteArrayInputStream(("hello"
					+ System.currentTimeMillis()).getBytes())), null);
			//after each file transfer it is required to reset the client with the following two
			//lines
			client.setPassive();
			client.setLocalActive();
			
			for(Object o : client.list()) {
				FileInfo fileInfo = (FileInfo) o;
				log.info("File {} of type {}", fileInfo.getName(), fileInfo.isDirectory()
						? "directory" : "file");
			}
		} finally {
			client.close();
		}
		
		log.info("GridFTP operations took {} ms", Duration.between(start,
				Instant.now()).toMillis());
	}
}