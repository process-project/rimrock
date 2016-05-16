package pl.cyfronet.rimrock.controllers.rest.proxygeneration;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import pl.cyfronet.rimrock.controllers.rest.jobs.ValidationException;
import pl.cyfronet.rimrock.gridworkerapi.service.JSagaExtras;

@Controller
public class ProxyGenerationController {
	private static final Logger log = LoggerFactory.getLogger(ProxyGenerationController.class);
	
	public static final String PROXY_GENERATION_PATH = "/api/userproxy";
	
	public static final String USER_CREDENTIALS = "MISSING (proxy generation)";
	
	public static final String USER_LOGIN_HEADER_NAME = "USER-LOGIN";
	
	public static final String USER_PASSWORD_HEADER_NAME = "USER-PASSWORD";
	
	public static final String PRIVATE_KEY_PASSWORD_HEADER_NAME = "PRIVATE-KEY-PASSWORD";
	
	@Value("${keyfs.cyfronet.prefix.cert.template}")
	private String keyFsPrefixCertTemplate;
	
	@Value("${keyfs.cyfronet.prefix.key.template}")
	private String keyFsPrefixKeyTemplate;
	
	@Value("${keyfs.host}")
	private String keyFsHost;
	
	private JSagaExtras jSagaService;

	private BanUtil banUtil;
	
	private class KeyFsCredentials {
		String cert;
		String key;
	}
	
	@Autowired
	public ProxyGenerationController(@Qualifier("jsaga") JSagaExtras gridWorkerService,
			BanUtil banUtil) {
		this.jSagaService = gridWorkerService;
		this.banUtil = banUtil;
	}
	
	@RequestMapping(value = PROXY_GENERATION_PATH, method = GET)
	@ResponseBody
	public ResponseEntity<String> generateProxy(
			@RequestHeader(USER_LOGIN_HEADER_NAME)
			Optional<String> userLogin,
			@RequestHeader(USER_PASSWORD_HEADER_NAME)
			Optional<String> basedUserPassword,
			@RequestHeader(PRIVATE_KEY_PASSWORD_HEADER_NAME)
			Optional<String> basedPrivateKeyPassword)
					throws BanException, ProxyGenerationException {
		log.info("Proxy generation request for user {} started",
				userLogin.orElse("missing user login"));
		
		String proxy = null;
		
		if (userLogin.isPresent() && basedUserPassword.isPresent()
				&& basedPrivateKeyPassword.isPresent()) {
			if (!banUtil.canProceed(userLogin.get())) {
				throw new BanException("Locked for another "
						+ banUtil.getDurationSeconds(userLogin.get()) + " seconds");
			}
			
			try {
				Instant t1 = Instant.now();
				byte[] userPassword = Base64.getDecoder().decode(basedUserPassword.get()); 
				byte[] privateKeyPassword = Base64.getDecoder().decode(
						basedPrivateKeyPassword.get());
				KeyFsCredentials keyFsCredentials = retrieveKeyFsCredentials(
						userLogin.get(), userPassword);
				proxy = jSagaService.generateProxy(
						keyFsCredentials.cert, keyFsCredentials.key, privateKeyPassword);
				log.info("Proxy generation for user {} completed in {} ms",
						userLogin.orElse("missing user login"),
						Duration.between(t1, Instant.now()).toMillis());
				banUtil.success(userLogin.get());
			} catch (IllegalArgumentException e) {
				String msg = "User password or private key password were not properly encoded with"
						+ " the base64 method";
				log.error(msg, e);
				banUtil.failure(userLogin.get());
				
				throw new ValidationException(msg);
			} catch (JSchException | SftpException e) {
				String msg = "User key or certificate were not found in a typical KeyFS location";
				log.error(msg, e);
				banUtil.failure(userLogin.get());
				
				throw new ProxyGenerationException();
			} catch (RemoteException e) {
				String msg = "Proxy certificate could not be generated";
				log.error(msg, e);
				banUtil.failure(userLogin.get());
				
				throw new ProxyGenerationException();
			}
		} else {
			throw new ValidationException("User login, user password and private key password "
					+ "are required to perform proxy certificate generation");
		}
		
		return new ResponseEntity<String>(proxy, OK);
	}

	private KeyFsCredentials retrieveKeyFsCredentials(String userLogin, byte[] userPassword)
			throws JSchException, SftpException {
		KeyFsCredentials result = new KeyFsCredentials();
		ChannelSftp channel = null;
		Session session = null;
		
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(userLogin, keyFsHost, 22);
			session.setPassword(userPassword);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			
			channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();
			
			String certPath = keyFsPrefixCertTemplate.replace("{userLogin}", userLogin);
			String keyPath = keyFsPrefixKeyTemplate.replace("{userLogin}", userLogin);
			result.cert = new BufferedReader(new InputStreamReader(channel.get(certPath)))
					.lines().collect(Collectors.joining("\n"));
			result.key = new BufferedReader(new InputStreamReader(channel.get(keyPath)))
					.lines().collect(Collectors.joining("\n"));
		} finally {
			if (channel != null) {
				channel.disconnect();;
			}
			
			if (session != null) {
				session.disconnect();
			}
		}
		
		return result;
	}
}