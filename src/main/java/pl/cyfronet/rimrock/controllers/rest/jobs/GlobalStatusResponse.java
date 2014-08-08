package pl.cyfronet.rimrock.controllers.rest.jobs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GlobalStatusResponse {
	@JsonProperty("error_message") private String errorMessage;
	private String status;
	private List<StatusResponse> statuses;
	
	public GlobalStatusResponse(String status, String errorMessage, List<StatusResponse> statuses) {
		this.status = status;
		this.errorMessage = errorMessage;
		this.statuses = statuses;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public List<StatusResponse> getStatuses() {
		return statuses;
	}
	public void setStatuses(List<StatusResponse> statuses) {
		this.statuses = statuses;
	}
}