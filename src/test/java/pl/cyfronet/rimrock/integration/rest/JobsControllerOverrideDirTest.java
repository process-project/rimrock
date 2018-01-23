package pl.cyfronet.rimrock.integration.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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
import pl.cyfronet.rimrock.controllers.rest.jobs.JobInfo;
import pl.cyfronet.rimrock.controllers.rest.jobs.SubmitRequest;
import pl.cyfronet.rimrock.gsi.ProxyHelper;

@RunWith(Parameterized.class)
@SpringBootTest(classes = RimrockApplication.class)
@WebAppConfiguration
@DirtiesContext
public class JobsControllerOverrideDirTest {
	private static final Logger log = LoggerFactory.getLogger(JobsControllerMvcTest.class);

	@Value("${test.user.login}")
    private String userLogin;

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

	private MockMvc mockMvc;

	private String host;

	private String script;

	private String workingDirectory;

	@Parameters
    public static Collection<Object[]> data() {
    	return Fixtures.jobWithDirOverrideParameters();
    }

    public JobsControllerOverrideDirTest(String host, String script, String workingDirectory) {
		this.host = host;
		this.script = script;
		this.workingDirectory = workingDirectory;
	}

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

	@Test
	public void testOverrideWorkingDirectory() throws JsonProcessingException, Exception {
		SubmitRequest submitRequest = new SubmitRequest();
		submitRequest.setHost(host);
		submitRequest.setScript(script);
		submitRequest.setWorkingDirectory(getWorkingDirectory());

		MvcResult result = mockMvc.perform(post("/api/jobs")
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy()))
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsBytes(submitRequest)))

				.andDo(print())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		JobInfo submitResult = mapper.readValue(body, JobInfo.class);
		String jobId = submitResult.getJobId();
		log.info("Checking job status for job id {}", jobId);

		mockMvc.perform(get("/api/jobs/" + jobId)
				.header("PROXY", proxyHelper.encodeProxy(proxyFactory.getProxy())))

				.andDo(print())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	private String getWorkingDirectory() {
	   return workingDirectory.replaceAll("\\{userLogin\\}", userLogin);
    }
}