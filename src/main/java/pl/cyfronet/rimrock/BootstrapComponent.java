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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import pl.cyfronet.rimrock.domain.GridJob;
import pl.cyfronet.rimrock.domain.GridJob.Middleware;
import pl.cyfronet.rimrock.repositories.GridJobRepository;

/**
 * Contains code which should be run only once after application context is created.
 *
 */
@Component
public class BootstrapComponent implements ApplicationListener<ContextRefreshedEvent>{
	private static final Logger log = LoggerFactory.getLogger(BootstrapComponent.class);
	
	@Value("classpath:certs/TERENASSLCA")	private Resource terenaCert;
	@Value("classpath:certs/SIMPLECA") private Resource simpleCaCert;
	
	@Autowired GridJobRepository gridJobRepository;
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		//see https://www.aquaclusters.com/app/home/project/public/aquadatastudio/discussion/GeneralDiscussions/post/25/Java-6u29-bug-prevents-SSL-connection-to-SQL-Server-2008-R2
		System.setProperty("jsse.enableCBCProtection", "false");
		
		try {
			enableTrustedSSL();
		} catch (Exception e) {
			log.error("Could not properly configure trust manager", e);
		}
		
		doDbMigration();
	}
	
	private void doDbMigration() {
		log.info("Performing grid job dn migration...");
		
		for(GridJob gridJob : gridJobRepository.findAll()) {
			log.info("Updating grid job with id {} and native job id {}", gridJob.getId(), gridJob.getNativeJobId());
			
			if(gridJob.getNativeJobId() != null && !gridJob.getNativeJobId().startsWith("http")) {
				gridJob.setMiddleware(Middleware.qcg);
				log.info("Setting qcg middleware value");
			} else {
				gridJob.setMiddleware(Middleware.jsaga);
				log.info("Setting jsaga middleware value");
			}
			
			gridJobRepository.save(gridJob);
		}
		
		log.info("Migration finished.");
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