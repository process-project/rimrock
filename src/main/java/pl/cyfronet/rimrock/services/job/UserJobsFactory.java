package pl.cyfronet.rimrock.services.job;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;

import org.globus.gsi.CredentialException;
import org.ietf.jgss.GSSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;
import pl.cyfronet.rimrock.services.filemanager.FileManagerFactory;
import pl.cyfronet.rimrock.services.ssh.GsisshRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserJobsFactory {

	@Autowired
	private FileManagerFactory fileManagerFactory;
	
	@Autowired
	private GsisshRunner runner;
	
	@Autowired
	private JobRepository jobRepository;
	
	@Autowired
	private ProxyHelper proxyHelper;
	
	@Autowired
	private ObjectMapper mapper;

	public UserJobs get(String proxy) throws CredentialException, GSSException, KeyStoreException, CertificateException, IOException {
		return new UserJobs(proxy, fileManagerFactory, runner, jobRepository,
				proxyHelper, mapper);
	}
}
