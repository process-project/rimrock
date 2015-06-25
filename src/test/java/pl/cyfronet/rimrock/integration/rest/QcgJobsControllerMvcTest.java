package pl.cyfronet.rimrock.integration.rest;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.jobs.JobActionRequest;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext
@Ignore("Takes too long")
public class QcgJobsControllerMvcTest {
	private static final Logger log = LoggerFactory.getLogger(QcgJobsControllerMvcTest.class);
	
	@Autowired private ProxyFactory proxyFactory;
	@Autowired private ProxyHelper proxyHelper;
	
	@Value("${local.server.port}") private int serverPort;
	
	@Before
	public void setup() {
		RestAssured.port = serverPort;
		String finalServerAddress = "http://localhost:" + serverPort;
		RestAssured.baseURI = finalServerAddress;
		log.info("Server address used: {}", finalServerAddress);
	}
	
	@Test
	public void testJobLifecycle() throws Exception {
		String proxy = proxyFactory.getProxy();
		
		String jobId =
			given().
				header("PROXY", proxyHelper.encodeProxy(proxy)).
				contentType("multipart/form-data").
				multiPart("files", "in1.txt", "First file".getBytes(), "text/plain").
				multiPart("files", "in2.txt", "Second file".getBytes(), "text/plain").
				multiPart("files", "run.sh", "#!/bin/bash\ncat in1.txt in2.txt > result.txt\ndate\n".getBytes(), "text/plain").
				formParam("executable", "run.sh").
				formParam("stdOutput", "out.txt").
				formParam("stdError", "error.txt").
				formParam("outputSandbox", "result.txt").
				formParam("candidateHosts", "creamce.inula.man.poznan.pl").
			when().
				post("/api/qcgjobs").
			then().
				log().all().
				contentType(JSON).
				statusCode(201).
			extract().
				path("job_id");
		
		log.info("Job id is {}", jobId);
		assertNotNull(jobId);
		
		given().
			header("PROXY", proxyHelper.encodeProxy(proxy)).
		when().
			get("/api/qcgjobs").
		then().
			log().all().
			contentType(JSON).
			statusCode(200);
		
		String status = null;
		
		while(status == null || !Arrays.asList(new String[] {"FAILED", "DONE"}).contains(status)) {
			status =
				given().
					header("PROXY", proxyHelper.encodeProxy(proxy)).
				when().
					get("/api/qcgjobs/" + jobId).
				then().
					log().all().
					contentType(JSON).
					statusCode(200).
				extract().
					path("status");
			Thread.sleep(2000);
		}
		
		assertEquals("DONE", status);
		
		given().
			header("PROXY", proxyHelper.encodeProxy(proxy)).
		when().
			get("/api/qcgjobs/" + jobId + "/files/result.txt").
		then().
			log().all().
			statusCode(200).
			body(equalTo("First fileSecond file"));
	}
	
	@Test
	public void testJobDelete() throws Exception {
		String proxy = proxyFactory.getProxy();
		
		String jobId =
				given().
					header("PROXY", proxyHelper.encodeProxy(proxy)).
					contentType(ContentType.URLENC).
					formParam("executable", "/bin/date").
					formParam("stdOutput", "out.txt").
					formParam("stdError", "error.txt").
				when().
					post("/api/qcgjobs").
				then().
					log().all().
					contentType(JSON).
					statusCode(201).
				extract().
					path("job_id");
		
		given().
			header("PROXY", proxyHelper.encodeProxy(proxy)).
		when().
			delete("/api/qcgjobs/" + jobId).
		then().
			statusCode(NO_CONTENT.value());
		
		given().
			header("PROXY", proxyHelper.encodeProxy(proxy)).
		when().
			get("/api/qcgjobs/" + jobId).
		then().
			log().all().
			statusCode(NOT_FOUND.value());
	}
	
	@Test
	public void testJobAbort() throws Exception {
		String proxy = proxyFactory.getProxy();
		
		String jobId =
				given().
					header("PROXY", proxyHelper.encodeProxy(proxy)).
					contentType(ContentType.URLENC).
					formParam("executable", "/bin/date").
					formParam("stdOutput", "out.txt").
					formParam("stdError", "error.txt").
				when().
					post("/api/qcgjobs").
				then().
					log().all().
					contentType(JSON).
					statusCode(201).
				extract().
					path("job_id");
		
		JobActionRequest jobActionRequest = new JobActionRequest();
		jobActionRequest.setAction("abort");
		given().
			header("PROXY", proxyHelper.encodeProxy(proxy)).
			contentType(JSON).
			body(jobActionRequest).
		when().
			put("/api/qcgjobs/" + jobId).
		then().
			log().all().
			statusCode(NO_CONTENT.value());
		
		String status = null;
		
		while(status == null || !"CANCELED".equals(status)) {
			status =
				given().
					header("PROXY", proxyHelper.encodeProxy(proxy)).
				when().
					get("/api/qcgjobs/" + jobId).
				then().
					log().all().
					contentType(JSON).
					statusCode(200).
				extract().
					path("status");
			Thread.sleep(2000);
		}
	}
}