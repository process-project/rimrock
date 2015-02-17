package pl.cyfronet.rimrock;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Contains code which should be run only once after application context is created.
 *
 */
@Component
public class BootstrapComponent implements ApplicationListener<ContextRefreshedEvent>{
	private static final Logger log = LoggerFactory.getLogger(BootstrapComponent.class);
	
	@Value("classpath:certs/TERENASSLCA")	private Resource terenaCert;
	@Value("classpath:certs/SIMPLECA") private Resource simpleCaCert;
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			enableTrustedSSL();
		} catch (Exception e) {
			log.error("Could not properly configure trust manager", e);
		}
	}
	
	private void enableTrustedSSL() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
		log.info("Configuring SSL context to trust TERENA and SIMPLECA");
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		Certificate terenaCertificate = certFactory.generateCertificate(terenaCert.getInputStream());
		Certificate simpleCaCertificate = certFactory.generateCertificate(simpleCaCert.getInputStream());
		
		KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
		store.load(null, null);
		store.setCertificateEntry("terena", terenaCertificate);
		store.setCertificateEntry("simpleca", simpleCaCertificate);
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(store);
		
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
			SSLContext.setDefault(sc);
		} catch (Exception e) {
			//ignoring
		}
	}
}