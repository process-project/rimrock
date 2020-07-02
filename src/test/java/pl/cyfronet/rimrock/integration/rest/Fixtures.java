package pl.cyfronet.rimrock.integration.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ObjectArrays;

public class Fixtures {
	private static final String grant = "plgdiceservices6";

	public static Collection<Object[]> jobParameters() {
		return
				Arrays.asList(new Object[][] {
					{
						"zeus.cyfronet.pl",
						"#!/bin/bash\n" +
						"#SBATCH -A " + grant + "\n" +
						"echo hello\n" +
						"exit 0"
					},
					{
						"prometheus.cyfronet.pl",
						"#!/bin/bash\n" +
						"#SBATCH -A " + grant + "\n" +
						"echo hello\n" +
						"exit 0"
					}
				});
	}

	public static Collection<Object[]> jobWithDirOverrideParameters() {
		String randomDirectory = UUID.randomUUID().toString();
		List<String> workingDirectories = Arrays.asList(
				"/people/{userLogin}/" + randomDirectory,
				"/net/people/{userLogin}/" + randomDirectory);
		List<Object[]> jobParameters = new ArrayList<>(jobParameters());
		Collection<Object[]> result = new ArrayList<>();

		for (int i = 0; i < workingDirectories.size(); i++) {
			jobParameters.get(i);
			result.add(ObjectArrays.concat(jobParameters.get(i), workingDirectories.get(i)));
		}

		return result;
	}
}
