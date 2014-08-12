package pl.cyfronet.rimrock.controllers.rest.irun;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateRequest {
	@JsonProperty("standard_output") private String standardOutput;
	@JsonProperty("standard_error") private String standardError;
	
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
}