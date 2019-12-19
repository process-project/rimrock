package pl.cyfronet.rimrock.services.ssh;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import pl.cyfronet.rimrock.gsi.ProxyHelper;

public abstract class SshRunnerBase {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected class NormalizedOutput {
		String output;
		int exitCode;
	}

	@Value("${run.timeout.millis}")
	int runTimeoutMillis;

	@Value("${gsissh.pool.size}")
	int poolSize;

	@Autowired
	ProxyHelper proxyHelper;

	protected Map<String, AtomicInteger> logins;
	
	public SshRunnerBase() {
		logins = new HashMap<>();
		initialize();
	}
	
	protected abstract void initialize();
	
	protected void checkPool(String userLogin) throws InterruptedException {
		synchronized(logins) {
			if(!logins.containsKey(userLogin)) {
				logins.put(userLogin, new AtomicInteger(0));
			}

			while(logins.get(userLogin).get() >= poolSize) {
				log.debug("Thread {} awaits for {} execution", Thread.currentThread().getId(), getClass().getName());
				logins.wait();
			}

			log.debug("Thread {} granted {} execution", Thread.currentThread().getId(), getClass().getName());
			logins.get(userLogin).incrementAndGet();
		}
	}

	protected void freePool(String userLogin) {
		synchronized(logins) {
			log.debug("Thread {} frees {} execution", Thread.currentThread().getId(), getClass().getName());

			int size = logins.get(userLogin).decrementAndGet();

			if(size == 0) {
				logins.remove(userLogin);
			}

			logins.notify();
		}
	}
	
	protected NormalizedOutput normalizeStandardOutput(String output, String separator) {
		log.trace("Output being normalized: {}", output);

		NormalizedOutput result = new NormalizedOutput();

		//matching proper output
		Pattern pattern = Pattern.compile(".*^" + separator + "$\\s+(.*)^(.*?)\\s+^" + separator + "$.*",
				Pattern.MULTILINE | Pattern.DOTALL);
		Matcher matcher = pattern.matcher(output);

		if(matcher.matches()) {
			result.output = matcher.group(1).replaceAll("\r\n", "\n").trim();

			try {
				result.exitCode = Integer.parseInt(matcher.group(2));
			} catch(NumberFormatException e) {
				log.warn("Exit code {} could not be parsed");
			}
		} else {
			//trying to match everything after the first separator (in case a timeout occurred)
			Pattern fallbackPattern = Pattern.compile(".*^" + separator + "$\\s+(.*)",
					Pattern.MULTILINE | Pattern.DOTALL);
			matcher = fallbackPattern.matcher(output);

			if(matcher.matches()) {
				result.output = matcher.group(1).replaceAll("\r\n", "\n").trim();
			}
		}

		return result;
	}
	
	protected byte[] completeCommand(String command, String separator, String workingDirectory) {
		return ("unset HISTFILE; "
				+ "echo '" + separator + "'; "
				+ (workingDirectory != null ? "cd " + workingDirectory + "; " : "")
				+ command + "; echo $?; "
				+ "echo '" + separator + "'; "
				+ "exit\n")
				.getBytes();
	}
}
