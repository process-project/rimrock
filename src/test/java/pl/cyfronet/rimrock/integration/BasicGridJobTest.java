package pl.cyfronet.rimrock.integration;

import javax.xml.ws.BindingProvider;

import org.junit.Ignore;
import org.junit.Test;

import pl.cyfronet.rimrock.cream.AuthorizationFault_Exception;
import pl.cyfronet.rimrock.cream.CREAM;
import pl.cyfronet.rimrock.cream.CREAMPort;
import pl.cyfronet.rimrock.cream.GenericFault_Exception;
import pl.cyfronet.rimrock.cream.InvalidArgumentFault_Exception;
import pl.cyfronet.rimrock.cream.JobListResponse;
import pl.cyfronet.rimrock.cream.JobSubmissionDisabledFault_Exception;

public class BasicGridJobTest {
	@Test
	@Ignore
	public void testGridJob() throws AuthorizationFault_Exception, GenericFault_Exception, InvalidArgumentFault_Exception, JobSubmissionDisabledFault_Exception {
		CREAM cream = new CREAM();
		CREAMPort port = cream.getCREAM2();
		
		BindingProvider bindingProvider = (BindingProvider) port;
		bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://cream.grid.cyf-kr.edu.pl:8443/ce-cream/services");
		
		JobListResponse jobListResponse = port.jobList();
		System.out.println(jobListResponse);
	}
}