package pl.cyfronet.rimrock.integration.rest;

import static com.jayway.restassured.RestAssured.given;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import pl.cyfronet.rimrock.RimrockApplication;

@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebIntegrationTest
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
			.header("USER_LOGIN", userLogin)
			.header("USER_PASSWORD",
					Base64.getEncoder().encodeToString(userPassword.getBytes()))
			.header("PRIVATE_KEY_PASSWORD",
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
		log.info("Obtained proxy is {}", proxy);
	}
	
	@Test
	public void testInvalidPassword() {
		given()
			.header("USER_LOGIN", userLogin)
			.header("USER_PASSWORD", "badPassword")
			.header("PRIVATE_KEY_PASSWORD", "badKeyPassword")
		.when()
			.get("/api/userproxy")
		.then()
			.log().all()
			.contentType(ContentType.JSON)
			.statusCode(404);
	}
}