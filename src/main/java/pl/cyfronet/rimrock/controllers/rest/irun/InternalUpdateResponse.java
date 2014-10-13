package pl.cyfronet.rimrock.controllers.rest.irun;

public class InternalUpdateResponse {
	private String input;
	
	public InternalUpdateResponse(String input) {
		this.input = input;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}
}