package pl.cyfronet.rimrock.gsi;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.globus.gsi.CredentialException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.RimrockApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class GsiCertTest {
	@Autowired ProxyHelper proxyHelper;
	
	@Value("classpath:pkcs8-proxy.pem") private Resource pkcs8Proxy;
	@Value("classpath:regular-proxy.pem") private Resource regularProxy;
	
	@Test
	public void testLoadingPkcs8BasedProxy() throws CredentialException, IOException {
		String login = proxyHelper.getUserLogin(IOUtils.toString(pkcs8Proxy.getInputStream()));
		assertTrue(login + " does not match plg.*", login.matches("plg.*"));
	}
	
	@Test
	public void testLoadingRegularProxy() throws CredentialException, IOException {
		String login = proxyHelper.getUserLogin(IOUtils.toString(regularProxy.getInputStream()));
		assertTrue(login + " does not match plg.*", login.matches("plg.*"));
	}
}