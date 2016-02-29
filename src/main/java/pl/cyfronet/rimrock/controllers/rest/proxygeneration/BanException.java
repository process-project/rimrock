package pl.cyfronet.rimrock.controllers.rest.proxygeneration;

public class BanException extends Exception {
	private static final long serialVersionUID = -2326846656720718076L;
	
	public BanException(String message) {
		super(message);
	}
}