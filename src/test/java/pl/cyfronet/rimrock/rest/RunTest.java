package pl.cyfronet.rimrock.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
public class RunTest {
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
		runRequest.setProxy(new String(Files.readAllBytes(Paths.get(proxyPath))));
		
		mockMvc.perform(post("/run")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.exitCode", is("0")));
	}
}