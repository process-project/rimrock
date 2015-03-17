package pl.cyfronet.rimrock;

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

public class JSagaGridWorkerServer implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(JSagaGridWorkerServer.class);
	
	private Process gridWorkerProcess;
	
	@Value("${jsaga.jar.version}") private String jSagaGridWorkerVersion;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("Starting JSaga grid worker instance with version {}...", jSagaGridWorkerVersion);
		File dir = new File("jsaga-grid-worker");
		String jarName = "jsaga-grid-worker-" + jSagaGridWorkerVersion + ".jar";
		
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		ClassPathResource jarResource = new ClassPathResource("workers/" + jarName);
		
		if(jarResource.exists()) {
			Files.copy(jarResource.getInputStream(), Paths.get(dir.getAbsolutePath(), jarName), StandardCopyOption.REPLACE_EXISTING);
			String[] command = new String[] {"java", "-jar", jarName};
			log.debug("Executing jsaga grid worker with command {}", command.toString());
			
			File logs = new File(dir, "jsaga-worker.log");
			ProcessBuilder pb = new ProcessBuilder(command).
					directory(new File(dir.getAbsolutePath())).
					redirectError(logs).
					redirectOutput(logs);
			gridWorkerProcess = pb.start();
			Thread.sleep(10000);
			log.info("JSaga grid worker instance successfully started");
		} else {
			throw new RuntimeException("JSaga grid worker jar file is missing. Cannot start.");
		}
	}
	
	@PreDestroy
	void close() throws InterruptedException {
		log.info("Shutting down JSaga grid worker instance...");
		gridWorkerProcess.destroy();
		Thread.sleep(5000);
		log.info("JSaga grid worker instance shutdown was successful");
	}
}