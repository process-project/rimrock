package pl.cyfronet.rimrock.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class InteractiveProcess {
	@Id @GeneratedValue private Long id;
	private String processId;
	private String output;
	private String error;
	private String pendingInput;
	private boolean finished;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getProcessId() {
		return processId;
	}
	public void setProcessId(String processId) {
		this.processId = processId;
	}
	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getPendingInput() {
		return pendingInput;
	}
	public void setPendingInput(String pendingInput) {
		this.pendingInput = pendingInput;
	}
	public boolean isFinished() {
		return finished;
	}
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
}