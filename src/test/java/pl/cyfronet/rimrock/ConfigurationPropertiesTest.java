package pl.cyfronet.rimrock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RimrockApplication.class)
public class ConfigurationPropertiesTest {
	@Autowired MonitoringAccounts monitoringAccounts;
	
	@Test
	public void testConfigurationProperties() {
		assertNotNull(monitoringAccounts.getMapping());
		assertTrue(monitoringAccounts.getMapping().size() > 0);
	}
}