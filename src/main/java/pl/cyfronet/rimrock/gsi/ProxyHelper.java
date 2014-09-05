package pl.cyfronet.rimrock.gsi;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.springframework.stereotype.Service;

@Service
public class ProxyHelper {
	public String getUserLogin(String proxyValue) throws CredentialException, GSSException {
		X509Credential proxy = new X509Credential(new ByteArrayInputStream(proxyValue.getBytes()));
		GSSCredential gsscredential = new GlobusGSSCredentialImpl(proxy, GSSCredential.INITIATE_ONLY);
		String dn = gsscredential.getName().toString();
		Pattern pattern = Pattern.compile(".*=(.*)$");
		Matcher matcher = pattern.matcher(dn);
		
		if(matcher.matches()) {
			return matcher.group(1);
		} else {
			throw new IllegalArgumentException("Could not extract user name from the supplied user proxy");
		}
	}
	
	public String decodeProxy(String proxy) {
		return new String(Base64.getDecoder().decode(proxy), Charset.forName("utf-8"));
	}

	public String encodeProxy(String proxy) {
		return Base64.getEncoder().encodeToString(proxy.replaceAll("\n", "\n").getBytes());
	}
}