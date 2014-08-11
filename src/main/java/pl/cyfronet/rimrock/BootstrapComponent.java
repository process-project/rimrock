package pl.cyfronet.rimrock;

import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Contains code which should be run only once after application context is created.
 *
 */
@Component
public class BootstrapComponent implements ApplicationListener<ContextRefreshedEvent>{
	private static final Logger log = LoggerFactory.getLogger(BootstrapComponent.class);
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		enableSSL();
	}
	
	private void enableSSL() {
		log.info("Configuring SSL context to trust all server certificates");
		
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
		
					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}
		
					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}};

		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			//ignoring
		}
	}
}