package pl.cyfronet.rimrock.integration;

import static java.lang.Thread.sleep;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.cyfronet.rimrock.ProxyFactory;
import pl.cyfronet.rimrock.RimrockApplication;
import pl.cyfronet.rimrock.services.gsissh.GsisshRunner;
import pl.cyfronet.rimrock.services.gsissh.RunResults;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RimrockApplication.class)
public class ConcurrentSshSessionsTest {
	private static final String HOST = "zeus.cyfronet.pl";

	@Autowired ProxyFactory proxyFactory;
	@Autowired GsisshRunner runner;

	@Test
	public void testMultipleSessions() throws InterruptedException {
		List<Thread> threads = new ArrayList<>();

		for(int i = 0; i < 20; i++) {
			int index = i;

			Thread t = new Thread(() -> {
				try {
					RunResults result = runner.run(HOST, proxyFactory.getProxy(),
							"sleep 2; echo hello " + index , null, 20000);
					Assert.assertTrue(result.getOutput().startsWith("hello"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			threads.add(t);
			t.start();
			sleep(100);
		}

		for(Thread t : threads) {
			t.join();
		}
	}
}
