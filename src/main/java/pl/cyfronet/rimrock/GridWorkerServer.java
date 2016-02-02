package pl.cyfronet.rimrock;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;

import pl.cyfronet.rimrock.util.PortFinder;

public class GridWorkerServer implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(GridWorkerServer.class);
	
	private Process gridWorkerProcess;

	private String artifactName;
	private String artifactVersion;
	private boolean runExtracted;
	private int serverPort;
	
	public GridWorkerServer(String artifactName, String artifactVersion, boolean runExtracted) {
		this.artifactName = artifactName;
		this.artifactVersion = artifactVersion;
		this.runExtracted = runExtracted;
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
			serverPort = PortFinder.getFreePort();
			
			List<String> command = null;
			
			if(runExtracted) {
				String mainClass = extractJar(dir, jarName);
				command = getRunExtractedCommand(dir, mainClass);
			} else {
				command = getNormalCommand(jarName);
			}
			
			log.debug("Executing {} grid worker with command {}", artifactName, command.toString());
			
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

	private String extractJar(File dir, String jarName) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder("jar", "xf", jarName).
				directory(new File(dir.getAbsolutePath()));
		Process process = pb.start();
		process.waitFor();
		
		File manifest = new File(dir, "META-INF/MANIFEST.MF");
		
		if(manifest.isFile()) {
			//removing already extracted jar
			new File(dir, jarName).delete();
			
			//finding the main class to be run
			String manifestContents = new String(Files.readAllBytes(Paths.get(manifest.getAbsolutePath())));
			log.debug("Manifest contents: {}", manifestContents);
			
			Pattern pattern = Pattern.compile("Start-Class: (.*)");
			Matcher matcher = pattern.matcher(manifestContents);
			
			if(matcher.find()) {
				String mainClass = matcher.group(1);
				
				return mainClass;
			}
		}
		
		throw new IllegalArgumentException("Grid worker jar " + jarName + " does not contain a manifest file with the main class entry");
	}

	private List<String> getRunExtractedCommand(File dir, String mainClass) {
		File libs = new File(dir, "lib");
		String cp = "";
		
		if(libs.isDirectory()) {
			File[] jars = libs.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".jar");
				}
			});
			
			for(File jar : jars) {
				cp += ":" + libs.getName() + "/" + jar.getName();
			}
		}
		
		List<String> command = new ArrayList<>();
		command.addAll(asList("java", "-Djava.rmi.server.hostname=localhost", "-cp", cp, mainClass,
				"--rmi.registry.port=" + serverPort));
		
		return command;
	}

	private List<String> getNormalCommand(String jarName) {
		List<String> command = new ArrayList<>();
		command.addAll(asList("java", "-Djava.rmi.server.hostname=localhost", "-jar", jarName,
				"--rmi.registry.port=" + serverPort));
		
		return command;
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
}