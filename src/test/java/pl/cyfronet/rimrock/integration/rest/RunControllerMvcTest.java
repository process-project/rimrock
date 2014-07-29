package pl.cyfronet.rimrock.integration.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.run.RunRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * DH: Consider using https://code.google.com/p/rest-assured/ for RESt tests
 * 
 * @author daniel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebAppConfiguration
public class RunControllerMvcTest {
	@Autowired private WebApplicationContext wac;
	@Autowired ObjectMapper mapper;
	
	@Value("${test.proxy.path}") String proxyPath;
	
	private MockMvc mockMvc;
	
	@Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }
	
	@Test
	public void testSimpleRun() throws Exception {
		RunRequest runRequest = new RunRequest();
		runRequest.setCommand("pwd");
		runRequest.setHost("zeus.cyfronet.pl");
		runRequest.setProxy(getProxy());
		
		mockMvc.perform(post("/api/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status", is("ok")))
				.andExpect(jsonPath("$.exit_code", is(0)))
				.andExpect(jsonPath("$.standard_output", startsWith("/people")));
	}
	
	@Test
	public void testExitCodeAndStandardErrorPresentInsideStandardOutput() throws Exception {
		RunRequest runRequest = new RunRequest();
		//at least the second mkdir command will return a 1 exit code
		runRequest.setCommand("echo 'error' > /dev/stderr; mkdir /tmp/test; mkdir /tmp/test");
		runRequest.setHost("zeus.cyfronet.pl");
		runRequest.setProxy(getProxy());
		
		mockMvc.perform(post("/api/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.exit_code", is(1)))
				.andExpect(jsonPath("$.status", is("ok")))
				.andExpect(jsonPath("$.standard_output", startsWith("error")));
	}
	
	@Test
	public void testNotNullValidation() throws JsonProcessingException, Exception {
		RunRequest runRequest = new RunRequest();
		mockMvc.perform(post("/api/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.exit_code", is(-1)))
				.andExpect(jsonPath("$.standard_output", is(equalTo(null))))
				.andExpect(jsonPath("$.status", is("error")))
				.andExpect(jsonPath("$.error_message", containsString("proxy:")))
				.andExpect(jsonPath("$.error_message", containsString("host:")));
	}
	
	private String getProxy() throws IOException {
		return new String(Files.readAllBytes(Paths.get(proxyPath)));
	}
}