package pl.cyfronet.rimrock.gsi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	@Test
	public void testDnMatcher() {
		Pattern p = Pattern.compile(ProxyHelper.LOGIN_FROM_DN_PATTERN);
		Matcher m1 = p.matcher("C=PL,O=PL-Grid,O=Uzytkownik,O=PL-Grid,CN=Imie Nazwisko,CN=plglogin,CN=311707489");
		assertTrue(m1.matches());
		assertEquals("plglogin", m1.group(1));
		
		Matcher m2 = p.matcher("C=PL,O=PL-Grid,O=Uzytkownik,O=PL-Grid,CN=Imie Nazwisko,CN=plglogin");
		assertTrue(m2.matches());
		assertEquals("plglogin", m2.group(1));
	}
}