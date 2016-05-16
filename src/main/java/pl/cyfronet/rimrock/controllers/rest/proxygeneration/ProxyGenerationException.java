package pl.cyfronet.rimrock.controllers.rest.proxygeneration;

public class ProxyGenerationException extends Exception {
	private static final long serialVersionUID = 7745718977211561941L;

	public ProxyGenerationException() {
		super("Proxy could not be generated. Contact service administrators for more details.");
	}
}