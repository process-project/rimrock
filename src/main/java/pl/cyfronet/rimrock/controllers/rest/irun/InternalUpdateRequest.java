package pl.cyfronet.rimrock.controllers.rest.irun;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InternalUpdateRequest {
	@JsonProperty("standard_output") private String standardOutput;
	@JsonProperty("standard_error") private String standardError;
	@JsonProperty("secret") private String secret;
	private boolean finished;
	
	public String getStandardOutput() {
		return standardOutput;
	}
	public void setStandardOutput(String standardOutput) {
		this.standardOutput = standardOutput;
	}
	public String getStandardError() {
		return standardError;
	}
	public void setStandardError(String standardError) {
		this.standardError = standardError;
	}
	@Override
	public String toString() {
		return "UpdateRequest [standardOutput=" + standardOutput + ", standardError=" + standardError + "]";
	}
	public boolean isFinished() {
		return finished;
	}
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String processId) {
		this.secret = processId;
	}
}