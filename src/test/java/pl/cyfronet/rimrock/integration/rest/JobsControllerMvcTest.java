package pl.cyfronet.rimrock.integration.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.RestHelper;
import pl.cyfronet.rimrock.controllers.rest.jobs.SubmitRequest;
import pl.cyfronet.rimrock.controllers.rest.jobs.SubmitResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebAppConfiguration
public class JobsControllerMvcTest {
	private static final Logger log = LoggerFactory.getLogger(JobsControllerMvcTest.class);
	
	@Autowired private WebApplicationContext wac;
	@Autowired private ObjectMapper mapper;
	@Autowired private ProxyFactory proxyFactory;
	
	private MockMvc mockMvc;
	
	@Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
	
	@Test
	public void testSimpleJobSubmission() throws JsonProcessingException, Exception {
		SubmitRequest submitRequest = new SubmitRequest();
		submitRequest.setHost("zeus.cyfronet.pl");
		submitRequest.setScript("#!/bin/bash\n"
				+ "echo hello\n"
				+ "exit 0");
		
		MvcResult result = mockMvc.perform(post("/api/jobs")
				.header("PROXY", RestHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(submitRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		
		String body = result.getResponse().getContentAsString();
		SubmitResponse submitResult = mapper.readValue(body, SubmitResponse.class);
		String jobId = submitResult.getJobId();
		log.info("Checking job status for job id {}", jobId);
		
		mockMvc.perform(get("/api/jobs/" + jobId)
				.header("PROXY", RestHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
	
	@Test
	public void testStatusRetrievalForInvalidJobId() throws Exception {
		mockMvc.perform(get("/api/jobs/nonexisting_id", "utf-8")
				.header("PROXY", RestHelper.encodeProxy(proxyFactory.getProxy())))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status", is("ERROR")))
				.andExpect(status().isNotFound());
	}
}