package pl.cyfronet.rimrock.controllers.rest.irun;

public class UpdateResponse {
	private String input;
	
	public UpdateResponse(String input) {
		this.input = input;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}
}