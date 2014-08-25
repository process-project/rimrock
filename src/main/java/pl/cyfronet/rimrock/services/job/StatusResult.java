package pl.cyfronet.rimrock.services.job;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatusResult {
	@JsonProperty("error_message") private String errorMessage;
	private List<Status> statuses;
	private String result;
	
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public List<Status> getStatuses() {
		return statuses;
	}
	public void setStatuses(List<Status> statuses) {
		this.statuses = statuses;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
}