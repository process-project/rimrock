package pl.cyfronet.rimrock.integration.rest;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.run.RunRequest;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

/**
 * DH: Consider using https://code.google.com/p/rest-assured/ for RESt tests
 * 
 * @author daniel
 *
 */
@RunWith(Parameterized.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebAppConfiguration
@DirtiesContext
public class RunControllerMvcTest {
	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
	
	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();
	
	@Autowired
	private WebApplicationContext wac;
	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private ProxyFactory proxyFactory;
	
	@Autowired
	private ProxyHelper proxyHelper;
	
	@Value("${run.timeout.millis}")
	private int runTimeoutMillis;
	
	private MockMvc mockMvc;
	
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
    
    public RunControllerMvcTest(String host) {
		this.host = host;
	}
	
	@Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

	@Test
	public void testSimpleRun() throws Exception {
		RunRequest runRequest = new RunRequest();
		runRequest.setCommand("pwd");
		runRequest.setHost(host);
		
		mockMvc.perform(post("/api/process")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("OK")))
				.andExpect(jsonPath("$.exit_code", is(0)))
				.andExpect(jsonPath("$.standard_output", anyOf(startsWith("/people"),
						startsWith("/net/people"), startsWith("/mnt/auto/people"))));
	}

	@Test
	public void testExitCodeAndStandardErrorPresent() throws Exception {
		RunRequest runRequest = new RunRequest();
		//at least the second mkdir command will return a 1 exit code
		runRequest.setCommand("echo 'error' > /dev/stderr; mkdir /tmp/test; mkdir /tmp/test");
		runRequest.setHost(host);
		
		mockMvc.perform(post("/api/process")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exit_code", is(1)))
				.andExpect(jsonPath("$.status", is("OK")))
				.andExpect(jsonPath("$.error_output", startsWith("error")));
	}
	
	@Test
	public void testNotNullValidation() throws JsonProcessingException, Exception {
		RunRequest runRequest = new RunRequest();
		mockMvc.perform(post("/api/process")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.exit_code", is(-1)))
				.andExpect(jsonPath("$.standard_output", is(equalTo(null))))
				.andExpect(jsonPath("$.status", is("ERROR")))
				.andExpect(jsonPath("$.error_message", containsString("host:")));
	}
	
	@Test
	public void testMultilineOutput() throws Exception {
		RunRequest runRequest = new RunRequest();
		runRequest.setCommand("echo hello1; echo hello2; echo hello3");
		runRequest.setHost(host);
		
		mockMvc.perform(post("/api/process")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("OK")))
				.andExpect(jsonPath("$.exit_code", is(0)))
				.andExpect(jsonPath("$.standard_output", is("hello1\nhello2\nhello3")));
	}
	
	@Test
	public void testTimeout() throws Exception {
		RunRequest runRequest = new RunRequest();
		runRequest.setCommand("echo 'going to sleep'; sleep " + ((runTimeoutMillis / 1000) + 5));
		runRequest.setHost(host);
		
		mockMvc.perform(post("/api/process")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(runRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isRequestTimeout())
				.andExpect(jsonPath("$.status", is("ERROR")))
				.andExpect(jsonPath("$.exit_code", is(-1)))
				.andExpect(jsonPath("$.standard_output", is("going to sleep")))
				.andExpect(jsonPath("$.error_message", startsWith("timeout")));
	}
}