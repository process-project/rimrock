package test

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

object ProcessSequence {
	val process =
		//sending single execution request
		exec(http("Process request")
			.post("/api/process")
			.body(StringBody("""{"host": "zeus.cyfronet.pl", "command": "pwd"}""")).asJSON
			.check(jsonPath("$.status").is("OK"), jsonPath("$.exit_code").ofType[Int].is(0))
		)
}

object IProcessSequence {
	val iprocess =
		//execution of interactive bash request
		exec(http("IProcess start bash")
			.post("/api/iprocesses")
			.body(StringBody("""{"host": "zeus.cyfronet.pl", "command": "bash"}""")).asJSON
			.check(jsonPath("$.status").is("OK"), jsonPath("$.process_id").saveAs("processId"))
		)
		//printing the session to see the 'processId' value for debugging
		.exec {session =>
			println(session)
			
			session
		}
		//checking the execution status with direct GET request
		.exec(http("IProcess direct GET")
			.get("/api/iprocesses/${processId}")
			.check(jsonPath("$.status").is("OK"))
		)
		//checking the execution status with list (all user processes) GET request
		.exec(http("IProcess list GET")
			.get("/api/iprocesses")
			.check(jsonPath("$[*]").count.greaterThan(0))
		)
		//providing input which should close the process
		.exec(http("IProcess input provision")
			.put("/api/iprocesses/${processId}")
			.body(StringBody("""{"standard_input": "exit"}""")).asJSON
			.check(jsonPath("$.status").is("OK"), jsonPath("$.finished").saveAs("finished"))
		)
		//waiting until the process finishes
		.asLongAs(session => !session("finished").as[String].equals("true")) {
			exec(http("IProcess looping until finished")
				.get("/api/iprocesses/${processId}")
				.check(jsonPath("$.status").is("OK"), jsonPath("$.finished").saveAs("finished"))
			)
			//printing the session to see the 'finished' value for debugging
			.exec {session =>
				println(session)
			
				session
			}
			.pause(1)
		}
}

object JobSequence {
	val job =
		//submitting new job
		exec(http("Job submission")
			.post("/api/jobs")
			.body(StringBody("""{"host": "zeus.cyfronet.pl", "script": "#!/bin/bash\necho hello\nexit 0"}""")).asJSON
			.check(jsonPath("$.status").saveAs("status"), jsonPath("$.job_id").saveAs("jobId"))
		)
		//printing the session to see the 'processId' value for debugging
		.exec {session =>
			println(session)
			
			session
		}
		//checking the job state with list (all user jobs) GET request
		.exec(http("Job status with list GET")
			.get("/api/jobs")
			.check(jsonPath("$[*]").count.greaterThan(0))
		)
		//waiting until the job reaches finished or aborted state
		.asLongAs(session => !session("status").as[String].equals("FINISHED")) {
			exec(http("Job status check for finished")
				.get("/api/jobs/${jobId}")
				.check(jsonPath("$.status").saveAs("status"))
			)
			.exec {session =>
				println(session)
			
				session
			}
			.pause(1)
		}
		//submitting another job to be aborted
		.exec(http("Job submission to test aborting job")
			.post("/api/jobs")
			.body(StringBody("""{"host": "zeus.cyfronet.pl", "script": "#!/bin/bash\necho hello\nexit 0"}""")).asJSON
			.check(jsonPath("$.status").saveAs("status"), jsonPath("$.job_id").saveAs("jobId"))
		)
		//waiting a little bit
		.pause(1)
		//aborting job
		.exec(http("Job abort")
			.put("/api/jobs/${jobId}")
			.body(StringBody("""{"action": "abort"}""")).asJSON
			.check(status.is(204))
		)
		//waiting until job status is aborted
		.asLongAs(session => !session("status").as[String].equals("ABORTED")) {
			exec(http("Job status check for aborted")
				.get("/api/jobs/${jobId}")
				.check(jsonPath("$.status").saveAs("status"))
			)
			.exec {session =>
				println(session)
			
				session
			}
			.pause(1)
		}
}

class RimrocTestSimulation extends Simulation {
	val baseUrl = "http://localhost:8080"
	
	val httpProtocol = http
		.baseURL(baseUrl)
		.header("PROXY", "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURmekNDQW1lZ0F3SUJBZ0lJVDNLZWJjWmM5Wk13RFFZSktvWklodmNOQVFFRkJRQXdkekVMTUFrR0ExVUUKQmhNQ1VFd3hFREFPQmdOVkJBb1RCMUJNTFVkeWFXUXhFekFSQmdOVkJBb1RDbFY2ZVhScmIzZHVhV3N4RVRBUApCZ05WQkFvVENFTlpSbEpQVGtWVU1SZ3dGZ1lEVlFRREV3OUVZVzVwWld3Z1NHRnlaWHBzWVdzeEZEQVNCZ05WCkJBTVRDM0JzWjJoaGNtVjZiR0ZyTUI0WERURTFNVEl3TnpBMk5Ua3lPRm9YRFRFMU1USXhOREEzTURReU9Gb3cKZ1ljeEN6QUpCZ05WQkFZVEFsQk1NUkF3RGdZRFZRUUtFd2RRVEMxSGNtbGtNUk13RVFZRFZRUUtFd3BWZW5sMAphMjkzYm1sck1SRXdEd1lEVlFRS0V3aERXVVpTVDA1RlZERVlNQllHQTFVRUF4TVBSR0Z1YVdWc0lFaGhjbVY2CmJHRnJNUlF3RWdZRFZRUURFd3R3Ykdkb1lYSmxlbXhoYXpFT01Bd0dBMVVFQXhNRmNISnZlSGt3Z2dFaU1BMEcKQ1NxR1NJYjNEUUVCQVFVQUE0SUJEd0F3Z2dFS0FvSUJBUUNENzl1a3h2eFZYcWF2cUxwQzRaeXBxR1c1MzN6VQptRUZhZUN3cHBSN3FBOGZxYXNpTFpIR05wOTNwTVBvTmp2NElSMWpUTVB3OFZ2VXBlUmJWckx5UXMyQ3pOMlp2CmV4cFdPYURMWm1TRFV6T2MvWW5oR0laVnJMMXBMSk1iQWF5RFlXdjNuQU5NbTJoRDNvL2M2U3l0cTl4aHM3M0IKZnpLMTdVTUxzMlJManZVSitaWVlFa0QwbFJQS0IzeU45dUtZRWRySHBDcE1UaUJxYlRhODBqUVVhSGpFMk1jZgorOGE2V0Jvc2M3S1RQTFhpQUc5Z0JBdTFNczJ0cEd0eTRjQWh4MFUvTFl1TG1zcGRaOTBpV1lPbmpLbXdGdDhYCk1DVUd4b0hidXgvalRJQW9UeWY5RG1FMzBCZ094dCtzQ2U4bGhZeU5RbXhocE9zT2xkTEdjWER0QWdNQkFBRXcKRFFZSktvWklodmNOQVFFRkJRQURnZ0VCQUJoclhobDFqSDhyQTdWMlBpbkxKNHBzcGxIQWpsbDhJNXJSQ29XbQozMk9SMWEzM3EzMGlrT2tFTXVCeXJLYndRZ3VyWDQrcFpaajFwUE92cDlRb2h4WHZaWjQzbmhPWE1LR01GaHFkClBURVdRbVF2cHJ0UHNhek44WU9CckV1a3htVkhaMW95QXZWLzRxMmd2Y1FwRXhGVjFLd24xVXBndEt4bWw4OGUKWitPZmFST2VwNnZtcUI2S1Y0ZWZTN1VzME1ieGo0Q3p0NUVTNDRTbVZVZ2pOaDRiVG1sbm9Ia1ZCVjZLdXN0VQpoMm1BNCt6WDF0OGt6S0E4RWtMK2lyZ1VZaFlpRlRrZnNHaTZjMHFaWXA1TTF0NmhnWjdjODlwRHd2UUNLY2pUCjFaZ09EbmJVMWtsMVNmL2xYQWN2SmlEbG9YcVpsU3NiOFd1L2YrbnFXcTc4YStzPQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCi0tLS0tQkVHSU4gUlNBIFBSSVZBVEUgS0VZLS0tLS0KTUlJRW93SUJBQUtDQVFFQWcrL2JwTWI4VlY2bXI2aTZRdUdjcWFobHVkOTgxSmhCV25nc0thVWU2Z1BINm1ySQppMlJ4amFmZDZURDZEWTcrQ0VkWTB6RDhQRmIxS1hrVzFheThrTE5nc3pkbWIzc2FWam1neTJaa2cxTXpuUDJKCjRSaUdWYXk5YVN5VEd3R3NnMkZyOTV3RFRKdG9ROTZQM09rc3JhdmNZYk85d1g4eXRlMURDN05rUzQ3MUNmbVcKR0JKQTlKVVR5Z2Q4amZiaW1CSGF4NlFxVEU0Z2FtMDJ2TkkwRkdoNHhOakhIL3ZHdWxnYUxIT3lrenkxNGdCdgpZQVFMdFRMTnJhUnJjdUhBSWNkRlB5MkxpNXJLWFdmZElsbURwNHlwc0JiZkZ6QWxCc2FCMjdzZjQweUFLRThuCi9RNWhOOUFZRHNiZnJBbnZKWVdNalVKc1lhVHJEcFhTeG5GdzdRSURBUUFCQW9JQkFFTm1uWWFvUVBBNzBsdWEKanN6c0JPU0hLckN5QTB0NEhLcmpDV3ljOWhZR3FIS1E2cktMTlpkd1VtVjJwOGVWYlNFOWtac1NRMGx3QXY2VQo1WjF6Q1VIQzRYdXNxWi81KzVKaERrdmFteTZicXBwZ3k3YzZtQ1hjZ1ViaGhxVWRUY3d3VUwvRjY4bU8rc3p4ClBmMVZBWVlYYmhaYmhHejlWaUtxZXIzWGZ6MDVwbCtIeVJUUXJqYVlkcHZtSllmVjcwUHNBUEM0OW5LYW5FcUcKSzA1emJ3NUxhS2NNSEljT0lNQWhXN3p0Q0pST0oxVHE0Y3l2QzJZdytHT0lEanpiYkVEWVgzTUZ4TFVUR29GRQpNaldla0ZOaU5rWTNrZWoxTk5hc29id1QwSWJ4bnlaL0Jxci8vVjAzNW5sQTYrODZ5SUZOcVVnRkpNM1dDWkxpCnJZTElkK0VDZ1lFQXlPY3owVDFzRitFaDczVEhWMHhHWXVLTW9yTkxreFI3ZFFKZWpoeTI4cStMTk4rcXFWeFIKV1dybTQzdldrZklvUWdiazR1VUh4NGxqSEsvRjVoNFlWTTJKZDFTZVhXRXhiVjkxMG1uS1lMOTdsKzUwcVlxWQozdzVXd3lLRGU5OVJ1ZWwrU1UrUFVYY1BQOEs2NEZPY0FucFRJdWtZcFpDTHZPRTB1RlBVb25NQ2dZRUFxQjYvCjFLbUQ4VnJZcFU4WTNPZ3JoVE5EUFYxOTZVZzFhR0tZSUUwckhpVjQ4L3pCY3hMTTdjaE11ZW9ieFNzbTlhTU8KNVcwWDJvemJKQ1JYb0NYTGZaWWw4Z3JSVHQ3T1RHR2FTOXFnR0swVUhYOTJsK2hxUkxlbE5JaDdvS0ZMYVlKcwpjdkM3dlgvZk4vV2QrUHVGU3pVbXRaSnZpU0pBRnV6ZmF3ZDA1eDhDZ1lFQXVTV0Jkb3FoakZ3NU53R3JHQU8rCk5qRk1wRUNTSEdqYTRkbWVKRi9JSmwycTc3NVUvQ0dOQXRmbkVxdzA4V2wydW1xNkUzUTR1Q1lnQmZiamJaQTgKb2lLTlZrRFFkWVExMWZNTWxTNVRpTmZPNGhTcXhQaHFxOTMrRFhWSU1TZDhuTlhYUVY2bDJORGRaOGhoQ28xagpFRkdGUG40TTdjQUpWN2RBSzhOdUNZc0NnWUI4OWtZdVEwWUFlejgzT0tESmFvVVd2L0RGeHgxVTdjaE02NlFaCnRtSkxTUjByZGY4d2twUGlWcFM1U0FzV05pb0NRUTVNZUJkWDZvVGt5MkthZGgxWUh1ZmJqakRnQi9xZGlaclAKc1JSNDR3VmxtNTNCOGc0elA3RzdaNHRFN1Q4SmtOa2RZbFNaMlZkSTEvZHczenZIbUIxS3J5cmFhcXZxbm15Kwp1RThyandLQmdEOVAwbWFXTWR2MEpndC9vV2IydWxwTHNHeXBXOHM3Wkc0N2NVRTM4MFYyMkVpN3Q4MzEwYURmCmJzL0VUQU9odGlLWVFvKzFTcDEweDVKZTlNL25rQllqQ1NGQWp6T1Z2Vlptb0RSWHRJd3VNclhVclg0WWsyTFYKOWVETG1UYUNlUWVhaldGZ0FwU2gvK1ZlbnNCUlhiOVJ0MUUza3lkalA1cUhaU2U2Z0JINwotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQotLS0tLUJFR0lOIENFUlRJRklDQVRFLS0tLS0KTUlJRStEQ0NBdUNnQXdJQkFnSUlUM0tlYmNaYzlaTXdEUVlKS29aSWh2Y05BUUVGQlFBd016RUxNQWtHQTFVRQpCaE1DVUV3eEVEQU9CZ05WQkFvVEIxQk1MVWR5YVdReEVqQVFCZ05WQkFNVENWTnBiWEJzWlNCRFFUQWVGdzB4Ck5UQTNNamN3T1RNNU1USmFGdzB4TmpBM01qWXdPVE01TVRKYU1IY3hDekFKQmdOVkJBWVRBbEJNTVJBd0RnWUQKVlFRS0V3ZFFUQzFIY21sa01STXdFUVlEVlFRS0V3cFZlbmwwYTI5M2JtbHJNUkV3RHdZRFZRUUtFd2hEV1VaUwpUMDVGVkRFWU1CWUdBMVVFQXhNUFJHRnVhV1ZzSUVoaGNtVjZiR0ZyTVJRd0VnWURWUVFERXd0d2JHZG9ZWEpsCmVteGhhekNDQVNJd0RRWUpLb1pJaHZjTkFRRUJCUUFEZ2dFUEFEQ0NBUW9DZ2dFQkFJYXpzemEzSEh6cTBUcXoKT3B1M09hWVhOWmRqZ2RzMzh0aWFVb3VjT2c0ZCtYbmxpMndxNW5oSDd5ZWp1TXpxY1dLWmVoOWpJQm1ISXdDSwplL1pwb3hwTDNCZDJUdFdBczR1NVl3T0ZLL1R3MWJnS0hMLzFDMGhxTERrbXB6c25zVlFPeXN3WkdXYW52VkRlCnRKem1xSlFtay9hVktFNU1KaERDRXlTL0F0STZhTlJjMFFBMUs0S3NOZTk2bXdValhPL1pwNi8rRWRGQmdCeUkKTlpWU1dqMzNoL0FLaTVmYkpXM3A1Y2xzVXZEaytjWkszVFNnOHVjSFFzV0lZcm9DV2NXZjR6RE5acVIzNDdPUAorZCszTUl3N3N2blp3bHJ4RWYyODZ3dEZMWWQ0ZTA3azlvMlZpM1FuMTJCN0ZFR0pTeUhLWGNYS1llOTFVVUNBCko4eDZiRTBDQXdFQUFhT0J5ekNCeURBZEJnTlZIUTRFRmdRVUJUb0IySFliNGlTOUlzWGhmZTArUW8zdnZOOHcKREFZRFZSMFRBUUgvQkFJd0FEQWFCZ05WSFNBRUV6QVJNQThHRFNzR0FRUUJncFl0QVFFQkFRSXdOd1lEVlIwZgpCREF3TGpBc29DcWdLSVltYUhSMGNEb3ZMM0JzWjNKcFpDMXpZMkV1ZDJOemN5NTNjbTlqTG5Cc0wyTnliQzVrClpYSXdId1lEVlIwakJCZ3dGb0FVcVJOUldXdEVTMytjb2ZjNXpOZ0IvRlJWUUMwd0V3WURWUjBsQkF3d0NnWUkKS3dZQkJRVUhBd0l3RGdZRFZSMFBBUUgvQkFRREFnU3dNQTBHQ1NxR1NJYjNEUUVCQlFVQUE0SUNBUUIyY2FCNgpjU0c5U0lRNXp6UXVMZXNnSmJQbnB5M21YMFdDOUFyMFpOdW82V1hRRHVTTDBaMTY0VFYwZ2FUbWE4NnJ4cXlzCjRXb2tRVmdiR3paajJvdmRFSlFhbnl0MlhpUFlORkxzSDlGSllRNkxGQ3lxTDNWcmNpVlIwZXNSRXl6anlZcWYKNXBUV0xrMzE5cnJDWHNsSDh1SGwwTXJzR2ZlQjJOOFVneVh5U0lTSE1WNFFMcE1tRllpUHA5TVZuNmJNaFlKYwpxSVJmbkRrcDUxVythR3Nocm1vbk83N3hISkg0UXlFWXJCNW1kZWlHak4rKzZwb3lVNVVhZ1Zkb0w2MkQyMDlkCmdDcTl0UVhnN2ZsNUtkR2J5LzB3STE0MUxRa2hGL2UwM3JJU2lHUHpEU1FvUWhZSkhGN3BCQ2hYTXd5L1pDMGQKdmVlcHRyQXBPTEFTMUVJN1I3RE1JbnpOSWJnaCszWnlCVDdpalJzaFh4WDNnbG5ZdTNhN2ROdXIwcnl0T21HaAprZWxuUEQ1MWF3dG1vVS91R2VCODRMaUhNWERidkRyQkZRVFp2VW0yNlo0WGFpdXg3OWVuRWE3NEErdnpEaHE3CnZrclk4RjVJU0UwZTdoSnlKQ1pNMWVlVlA4V29xUElJWG55bHJaUmhxd2g0OGZnbXlJYXllRllGRDRmenMwSzcKZE12MmM0eWdJcUlIOHltMWRPam5CbkZwZXhzNVVXc2JFSE05SngrMmF4cUE3VnR0RVYzTFp4L3JpYTg4SC9qbwpiZllMRUp4bVZvd1V5emlydmZHY29mRHB6TkJVVnhka0VEUGpaMzA4clJ1TU5QSVhhV0xXMGVENWdLQlQ5RFdHCllKNTI3bnk0Q25LSk5jOXhiRDZYWEZ4WWJYdkI2NjBZdUJUb2R3PT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=")
		.inferHtmlResources()
		.acceptHeader("*/*")
		.userAgentHeader("curl/7.37.1")

	val processScenario = scenario("Process scenario").repeat(100) { exec(ProcessSequence.process) }
			
	val iprocessScenario = scenario("IProcess scenario").exec(IProcessSequence.iprocess)
	
	val jobScenario = scenario("Job scenario").repeat(10) { exec(JobSequence.job) }

	setUp(
		processScenario.inject(atOnceUsers(1)),
		iprocessScenario.inject(atOnceUsers(1)),
		jobScenario.inject(atOnceUsers(1))
	).protocols(httpProtocol)
}