package pl.cyfronet.rimrock.integration.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.controllers.rest.jobs.JobActionRequest;
import pl.cyfronet.rimrock.controllers.rest.jobs.JobInfo;
import pl.cyfronet.rimrock.controllers.rest.jobs.SubmitRequest;
import pl.cyfronet.rimrock.domain.Job;
import pl.cyfronet.rimrock.gsi.ProxyHelper;
import pl.cyfronet.rimrock.repositories.JobRepository;

@RunWith(Parameterized.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
@WebAppConfiguration
@DirtiesContext
public class JobsControllerMvcTest {
	private static final Logger log = LoggerFactory.getLogger(JobsControllerMvcTest.class);
	
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
	
	@Autowired
	private JobRepository jobRepository;
	
	private MockMvc mockMvc;

	private String host;

	private String script;
	
	@Parameters
    public static Collection<Object[]> data() {
    	return Fixtures.jobParameters();
    }
    
    public JobsControllerMvcTest(String host, String script) {
		this.host = host;
		this.script = script;
	}
	
	@Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }
	
	@Test
	public void testSimpleJobSubmission() throws JsonProcessingException, Exception {
		SubmitRequest submitRequest = new SubmitRequest();
		submitRequest.setHost(host);
		submitRequest.setScript(script);
		
		MvcResult result = mockMvc.perform(post("/api/jobs")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(submitRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andReturn();
		
		String body = result.getResponse().getContentAsString();
		JobInfo submitResult = mapper.readValue(body, JobInfo.class);
		String jobId = submitResult.getJobId();
		log.info("Checking job status for job id {}", jobId);
		
		mockMvc.perform(get("/api/jobs/" + jobId)
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
	
	@Test
	public void testStatusRetrievalForInvalidJobId() throws Exception {
		mockMvc.perform(get("/api/jobs/nonexisting_id")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status", is("ERROR")))
				.andExpect(status().isNotFound());
	}
	
	@Test
	public void testRetrievalNotOwnedJob() throws Exception {
		Job job = new Job("not_owned_job", "FINISHED", "", "", "other_user_login", host, null);
		jobRepository.save(job);		
		
		mockMvc.perform(get("/api/jobs/not_owned_job")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())))
				.andExpect(status().isNotFound());
	}
	
	@Test
	public void testGlobalStatusRetrieval() throws Exception {
		mockMvc.perform(get("/api/jobs")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}
	
	@Test
	public void testJobDeleting() throws JsonProcessingException, Exception {
		SubmitRequest submitRequest = new SubmitRequest();
		submitRequest.setHost(host);
		submitRequest.setScript(script);
		
		MvcResult result = mockMvc.perform(post("/api/jobs")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(submitRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andReturn();
		
		String body = result.getResponse().getContentAsString();
		JobInfo submitResult = mapper.readValue(body, JobInfo.class);
		String jobId = submitResult.getJobId();
		log.info("Stopping job for job id {}", jobId);
		
		mockMvc.perform(delete("/api/jobs/" + jobId)
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())))
				
				.andDo(print())
				
				.andExpect(status().isNoContent());
	}
	
	@Test
	public void testJobAborting() throws JsonProcessingException, Exception {
		SubmitRequest submitRequest = new SubmitRequest();
		submitRequest.setHost(host);
		submitRequest.setScript(script);
		
		MvcResult result = mockMvc.perform(post("/api/jobs")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(submitRequest)))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andReturn();
		
		String body = result.getResponse().getContentAsString();
		JobInfo submitResult = mapper.readValue(body, JobInfo.class);
		String jobId = submitResult.getJobId();
		log.info("Aborting job for job id {}", jobId);
		
		JobActionRequest actionRequest = new JobActionRequest();
		actionRequest.setAction("abort");
		mockMvc.perform(put("/api/jobs/" + jobId)
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(actionRequest)))
				
				.andDo(print())
				
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/jobs/" + jobId)
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())))
				
				.andDo(print())
				
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status", is("ABORTED")))
				.andExpect(status().isOk());
	}
}