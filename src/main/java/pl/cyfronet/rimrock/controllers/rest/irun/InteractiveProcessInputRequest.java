package pl.cyfronet.rimrock.controllers.rest.irun;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InteractiveProcessInputRequest {
	@JsonProperty("standard_input")
	private String standardInput;

	public String getStandardInput() {
		return standardInput;
	}

	public void setStandardInput(String standardInput) {
		this.standardInput = standardInput;
	}
}