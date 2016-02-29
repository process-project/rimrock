package pl.cyfronet.rimrock.controllers.rest.proxygeneration;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.time.LocalTime.now;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BanUtil {
	@Value("${ban.util.duration.minutes}")
	private  int banDurationMinutes;
	
	@Value("${ban.util.attempts}")
	private int banAttempts;
	
	private Map<String, BanStatus> banStatuses;
	
	private class BanStatus {
		int attempts;
		
		LocalTime lastAttempt;
	}
	
	public BanUtil() {
		banStatuses = new HashMap<>();
	}
	
	public void success(String id) {
		if (banStatuses.containsKey(id)) {
			banStatuses.remove(id);
		}
	}
	
	public void failure(String id) {
		if (!banStatuses.containsKey(id)) {
			banStatuses.put(id, new BanStatus());
		}
		
		//clearing attempts after ban duration has passed
		if (banStatuses.get(id).attempts == banAttempts
				&& banStatuses.get(id).lastAttempt.isBefore(
						now().minusMinutes(banDurationMinutes))) {
			banStatuses.get(id).attempts = 0;
		}
		
		banStatuses.get(id).attempts = min(banStatuses.get(id).attempts + 1, banAttempts);
		banStatuses.get(id).lastAttempt = now();
	}
	
	public boolean canProceed(String id) {
		if (banStatuses.containsKey(id) && banStatuses.get(id).attempts == banAttempts
				&& banStatuses.get(id).lastAttempt.isAfter(
						now().minusMinutes(banDurationMinutes))) {
			return false;
		}
		
		return true;
	}

	public int getDurationSeconds(String id) {
		if (banStatuses.containsKey(id)) {
			return banDurationMinutes * 60 - abs(toIntExact(Duration.between(now(),
					banStatuses.get(id).lastAttempt).getSeconds()));
		}
		
		return -1;
	}
}