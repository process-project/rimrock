package pl.cyfronet.rimrock.integration.rest;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.irun.InteractiveProcessInputRequest;
import pl.cyfronet.rimrock.controllers.rest.irun.InteractiveProcessRequest;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebAppConfiguration
@IntegrationTest
public class InteractiveRunControllerTest {
	@Autowired private ProxyFactory proxyFactory;
	@Autowired private ProxyHelper proxyHelper;
	@Autowired private ObjectMapper mapper;
	
	@Value("${local.server.port}") private int serverPort;
	@Value("${server.address}") private String serverAddress;
	
	@Before
	public void setup() {
		RestAssured.port = serverPort;
		RestAssured.baseURI = "http://" + serverAddress + ":" + serverPort;
	}
	
	@Test
	public void testInteractiveRun() throws JsonProcessingException, Exception {
		InteractiveProcessRequest ipr = new InteractiveProcessRequest();
		ipr.setHost("ui.cyfronet.pl");
		ipr.setCommand("bash");
		
		String processId = 
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			contentType(JSON).
			body(mapper.writeValueAsBytes(ipr)).
		when().
			post("/api/iprocess").
		then().
			log().all().
			contentType(JSON).
			statusCode(200).
		extract().
			path("process_id");
		
		InteractiveProcessInputRequest ipir = new InteractiveProcessInputRequest();
		ipir.setStandardInput("echo 4\nexit");
		given().
			header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			contentType(JSON).
			body(mapper.writeValueAsBytes(ipir)).
		when().
			put("/api/iprocess/{id}", processId).
		then().
			log().all().
			contentType(JSON).
			statusCode(200);
		
		boolean finished = false;
		int attempts = 10;
		String output = "";
		
		while(!finished && attempts-- > 0) {
			Response response =
			given().
				header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())).
			when().
				get("/api/iprocess/{id}", processId).
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
}