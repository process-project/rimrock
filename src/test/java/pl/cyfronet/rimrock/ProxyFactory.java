package pl.cyfronet.rimrock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.X509Credential;
import org.globus.gsi.bc.BouncyCastleCertProcessingFactory;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;
import org.globus.gsi.util.CertificateLoadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ProxyFactory {
	private static final Logger log = LoggerFactory.getLogger(ProxyFactory.class);

	@Value("classpath:usercert.pem")
	private Resource userCertFile;

	@Value("classpath:userkey.pem")
	private Resource userKeyFile;

	@Value("${test.user.key.pass:}")
	private String userKeyPass;

	@Value("${test.proxy.path:}")
	private String proxyPath;

	private BouncyCastleCertProcessingFactory factory;
	private String proxy;

	public ProxyFactory() {
		factory = BouncyCastleCertProcessingFactory.getDefault();
	}

	public synchronized String getProxy() throws Exception {
		if (proxy == null) {
			if (proxyPath != null && !proxyPath.isEmpty()) {
				proxy = getProxyPayload();
			} else {
				proxy = generateProxy();
			}
		}

		return proxy;
	}

	private String generateProxy() throws GeneralSecurityException,
			IOException, Exception {
		log.info("Generating proxy");

		X509Certificate userCert = CertificateLoadUtil.loadCertificate(userCertFile.getInputStream());
		OpenSSLKey key = new BouncyCastleOpenSSLKey(userKeyFile.getInputStream());

		if (key.isEncrypted()) {
			try {
				key.decrypt(userKeyPass);
			} catch (GeneralSecurityException e) {
				throw new Exception("Wrong password or other security error");
			}
		}

		PrivateKey userKey = key.getPrivateKey();
		X509Credential credential = factory.createCredential(
				new X509Certificate[] {userCert}, userKey, 1024, 3600, GSIConstants.CertificateType.GSI_2_PROXY);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		credential.save(out);

		String proxyValue = new String(out.toByteArray());
		log.info("Generated proxy: {}", proxyValue);

		return proxyValue;
	}

	private String getProxyPayload() throws IOException {
		return new String(Files.readAllBytes(Paths.get(proxyPath)));
	}
}
