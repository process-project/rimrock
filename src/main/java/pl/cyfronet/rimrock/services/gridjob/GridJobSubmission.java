package pl.cyfronet.rimrock.services.gridjob;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class GridJobSubmission {
	private List<MultipartFile> files;
	private String executable;
	private String stdOutput;
	private String stdError;
	private List<String> outputSandbox;
	private String myProxyServer;
	private List<String> arguments;
	private List<String> candidateHosts;
	private String tag;

	public String getExecutable() {
		return executable;
	}

	public void setExecutable(String executable) {
		this.executable = executable;
	}

	public String getStdOutput() {
		return stdOutput;
	}

	public void setStdOutput(String stdOutput) {
		this.stdOutput = stdOutput;
	}

	public String getStdError() {
		return stdError;
	}

	public void setStdError(String stdError) {
		this.stdError = stdError;
	}

	public List<String> getOutputSandbox() {
		return outputSandbox;
	}

	public void setOutputSandbox(List<String> outputSandbox) {
		this.outputSandbox = outputSandbox;
	}

	public List<MultipartFile> getFiles() {
		return files;
	}

	public void setFiles(List<MultipartFile> files) {
		this.files = files;
	}

	public String getMyProxyServer() {
		return myProxyServer;
	}

	public void setMyProxyServer(String myProxyServer) {
		this.myProxyServer = myProxyServer;
	}

	public List<String> getCandidateHosts() {
		return candidateHosts;
	}

	public void setCandidateHosts(List<String> candidateHosts) {
		this.candidateHosts = candidateHosts;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}