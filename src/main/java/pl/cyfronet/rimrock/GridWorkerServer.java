package pl.cyfronet.rimrock;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;

public class GridWorkerServer implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(GridWorkerServer.class);
	
	private Process gridWorkerProcess;

	private String artifactName;
	private String artifactVersion;
	private int serverPort;
	
	public GridWorkerServer(String artifactName, String artifactVersion) {
		this.artifactName = artifactName;
		this.artifactVersion = artifactVersion;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("Starting {} grid worker instance with version {}...", artifactName, artifactVersion);
		File dir = new File(artifactName);
		String jarName = artifactName + "-" + artifactVersion + ".jar";
		
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		ClassPathResource jarResource = new ClassPathResource("workers/" + jarName);
		
		if(jarResource.exists()) {
			Files.copy(jarResource.getInputStream(), Paths.get(dir.getAbsolutePath(), jarName), StandardCopyOption.REPLACE_EXISTING);
			serverPort = getFreePort();
			String[] command = new String[] {"java", "-jar", jarName, "--rmi.registry.port=" + serverPort};
			log.debug("Executing {} grid worker with command {}", artifactName, Arrays.toString(command));
			
			File logs = new File(dir, artifactName + ".log");
			ProcessBuilder pb = new ProcessBuilder(command).
					directory(new File(dir.getAbsolutePath())).
					redirectError(logs).
					redirectOutput(logs);
			gridWorkerProcess = pb.start();
			log.info("{} grid worker instance successfully started", artifactName);
		} else {
			throw new RuntimeException(artifactName + " grid worker jar file is missing. Cannot start.");
		}
	}

	@PreDestroy
	void close() throws InterruptedException {
		log.info("Shutting down {} grid worker instance...", artifactName);
		gridWorkerProcess.destroy();
		log.info("{} grid worker instance shutdown was successful", artifactName);
	}
	
	public int getRegistryPort() {
		return serverPort;
	}
	
	private int getFreePort() throws IOException {
		ServerSocket socket = null;
		
		try {
			socket = new ServerSocket(0);
			
			return socket.getLocalPort();
		} finally {
			socket.close();
		}
	}
}