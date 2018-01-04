package pl.cyfronet.rimrock.integration.rest;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.globus.gsi.CredentialException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.proxygeneration.ProxyGenerationController;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

@SpringBootTest(classes = RimrockApplication.class, webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@FixMethodOrder(NAME_ASCENDING)
public class ProxyGenerationControllerTest {
	private static final Logger log = LoggerFactory.getLogger(ProxyGenerationControllerTest.class);
	
	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
	
	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();
	
	@Value("${test.user.login}")
	private String userLogin;
	
	@Value("${test.user.password}")
	private String userPassword;
	
	@Value("${test.user.key.pass:}")
	private String userKeyPass;
	
	@Value("${local.server.port}")
	private int serverPort;
	
	@Value("${test.server.bind.address}")
	private String serverAddress;
	
	@Autowired
	private ProxyHelper proxyHelper;
	
	@Before
	public void setup() {
		RestAssured.port = serverPort;
		String finalServerAddress = "http://" + serverAddress + ":" + serverPort;
		RestAssured.baseURI = finalServerAddress;
		log.info("Server address used: {}", finalServerAddress);
	}
	
	@Test
	public void testProxyGeneration() {
		Instant t1 = Instant.now();
		String proxy =
		given()
			.header(ProxyGenerationController.USER_LOGIN_HEADER_NAME, userLogin)
			.header(ProxyGenerationController.USER_PASSWORD_HEADER_NAME,
					Base64.getEncoder().encodeToString(userPassword.getBytes()))
			.header(ProxyGenerationController.PRIVATE_KEY_PASSWORD_HEADER_NAME,
					Base64.getEncoder().encodeToString(userKeyPass.getBytes()))
		.when()
			.get("/api/userproxy")
		.then()
			.log().all()
			.contentType(ContentType.TEXT)
			.statusCode(200)
		.extract()
			.body().asString();
		log.info("It took {} ms to generate proxy", Duration.between(t1, Instant.now()).toMillis());
		
		try {
			proxyHelper.verify(proxy);
		} catch (CredentialException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testThatAnyInvalidPasswordReturnsTheSameResponse() {
		Response response1 = generateProxy(
				Base64.getEncoder().encodeToString(userPassword.getBytes()),
				"badKeyPassword");
		Response response2 = generateProxy(
				"badPassword",
				Base64.getEncoder().encodeToString(userKeyPass.getBytes()));
		
		response1.then()
			.statusCode(422)
			.contentType(ContentType.JSON);
		
		assertEquals(response1.statusCode(), response2.statusCode());
		assertEquals(response1.body().asString(), response2.body().asString());
	}

	private Response generateProxy(String password, String keyPassword) {
		return
			given()
				.header(ProxyGenerationController.USER_LOGIN_HEADER_NAME, userLogin)
				.header(ProxyGenerationController.USER_PASSWORD_HEADER_NAME, password)
				.header(ProxyGenerationController.PRIVATE_KEY_PASSWORD_HEADER_NAME, keyPassword)
			.when()
				.get("/api/userproxy")
			.then()
				.log().all()
			.extract()
				.response();
	}
}