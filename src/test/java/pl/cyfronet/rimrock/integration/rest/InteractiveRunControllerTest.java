package pl.cyfronet.rimrock.integration.rest;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.irun.InteractiveProcessInputRequest;
import pl.cyfronet.rimrock.controllers.rest.irun.InteractiveProcessRequest;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

@RunWith(Parameterized.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebIntegrationTest
@DirtiesContext
@Ignore("public IP needed to run this test")
public class InteractiveRunControllerTest {
	private static final Logger log = LoggerFactory.getLogger(InteractiveRunControllerTest.class);

	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Autowired
	private ProxyFactory proxyFactory;

	@Autowired
	private ProxyHelper proxyHelper;

	@Autowired
	private ObjectMapper mapper;

	@Value("${local.server.port}")
	private int serverPort;

	@Value("${test.server.bind.address}")
	private String serverAddress;

	@Value("${irun.timeout.seconds}")
	private int irunTimeoutSeconds;

	private String host;

	@Parameters
    public static Collection<Object[]> data() {
    	return
			Arrays.asList(new Object[][] {
				{
					"zeus.cyfronet.pl"
				},
				{
					"prometheus.cyfronet.pl"
				}
			});
    }

    public InteractiveRunControllerTest(String host) {
		this.host = host;
	}

	@Before
	public void setup() {
		RestAssured.port = serverPort;
		String finalServerAddress = "http://" + serverAddress + ":" + serverPort;
		RestAssured.baseURI = finalServerAddress;
		log.info("Server address used: {}", finalServerAddress);
	}

	@Test
	public void testInteractiveRun() throws JsonProcessingException, Exception {
		InteractiveProcessRequest ipr = new InteractiveProcessRequest();
		ipr.setHost(host);
		ipr.setCommand("bash");

		String processId =
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			contentType(JSON).
			body(mapper.writeValueAsBytes(ipr)).
		when().
			post("/api/iprocesses").
		then().
			log().all().
			contentType(JSON).
			statusCode(201).
		extract().
			path("process_id");
		log.info("Obtained process id is {}", processId);

		InteractiveProcessInputRequest ipir = new InteractiveProcessInputRequest();
		ipir.setStandardInput("echo 4\nexit");
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			contentType(JSON).
			body(mapper.writeValueAsBytes(ipir)).
		when().
			put("/api/iprocesses/" + processId).
		then().
			log().all().
			contentType(JSON).
			statusCode(200);

		boolean finished = false;
		int attempts = 100;
		String output = "";

		while(!finished && attempts-- > 0) {
			Response response =
			given().
				header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			when().
				get("/api/iprocesses/" + processId).
			then().
				log().all().
				contentType(JSON).
				statusCode(200).
			extract().
				response();
			finished = response.<Boolean>path("finished");

			output += response.path("standard_output");

			//lets wait a bit
			Thread.sleep(200);
		}

		if(attempts < 0) {
			fail("Proper response could not be acquired in the defined number of attempts");
		}

		assertEquals("4", output.trim());
	}

	@Test
	public void testNonExistingIProcess() throws Exception {
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
		when().
			get("/api/iprocesses/nonExisting").
		then().
			log().all().
			contentType(JSON).
			statusCode(404);
	}

	@Test
	public void testTimeout() throws JsonProcessingException, Exception {
		InteractiveProcessRequest ipr = new InteractiveProcessRequest();
		ipr.setHost(host);
		ipr.setCommand("bash");

		String processId =
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			contentType(JSON).
			body(mapper.writeValueAsBytes(ipr)).
		when().
			post("/api/iprocesses").
		then().
			log().all().
			contentType(JSON).
			statusCode(201).
		extract().
			path("process_id");
		log.info("Obtained process id is {}", processId);

		Thread.sleep((irunTimeoutSeconds + 2) * 1000);

		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
		when().
			get("/api/iprocesses/" + processId).
		then().
			log().all().
			contentType(JSON).
			statusCode(200).
			body("standard_error", equalTo("Timeout occurred"));
	}

	@Test
	public void testMaxOutputBufferSize() throws JsonProcessingException, Exception {
		InteractiveProcessRequest ipr = new InteractiveProcessRequest();
		ipr.setHost("ui.cyfronet.pl");
		ipr.setCommand("bash");

		String processId =
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			contentType(JSON).
			body(mapper.writeValueAsBytes(ipr)).
		when().
			post("/api/iprocesses").
		then().
			log().all().
			contentType(JSON).
			statusCode(201).
		extract().
			path("process_id");
		log.info("Obtained process id is {}", processId);

		InteractiveProcessInputRequest ipir = new InteractiveProcessInputRequest();
		ipir.setStandardInput("printf \"%0.sa\" {1..50}\nexit"); //50 times 'a'
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			contentType(JSON).
			body(mapper.writeValueAsBytes(ipir)).
		when().
			put("/api/iprocesses/" + processId).
		then().
			log().all().
			contentType(JSON).
			statusCode(200);

		boolean finished = false;
		int attempts = 100;
		String output = "";

		while(!finished && attempts-- > 0) {
			Response response =
			given().
				header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			when().
				get("/api/iprocesses/" + processId).
			then().
				log().all().
				contentType(JSON).
				statusCode(200).
			extract().
				response();
			finished = response.<Boolean>path("finished");

			output += response.path("standard_output");

			//lets wait a bit
			Thread.sleep(200);
		}

		if(attempts < 0) {
			fail("Proper response could not be acquired in the defined number of attempts");
		}

		assertEquals(40, output.trim().length()); //the output should be truncated to 40 'a' characters
		assertTrue(output.trim().startsWith("aaa"));
		assertTrue(output.trim().endsWith("aaa"));
	}
}
