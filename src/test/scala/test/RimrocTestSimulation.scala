package test

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

object ProcessSequence {
	val process =
		exec(http("Process request")
			.post("/api/process")
			.body(StringBody("""{"host": "zeus.cyfronet.pl", "command": "pwd"}""")).asJSON
			.check(jsonPath("$.status").is("OK"), jsonPath("$.exit_code").ofType[Int].is(0))
		)
}

object IProcessSequence {
	val iprocess =
		exec(http("IProcess request 1")
			.post("/api/iprocess")
			.body(StringBody("""{"host": "zeus.cyfronet.pl", "command": "bash"}""")).asJSON
			.check(jsonPath("$.status").is("OK"), jsonPath("$.process_id").saveAs("processId"))
		)
		//printing the session to see the 'processId' value for debugging
		.exec {session =>
			println(session)
			
			session
		}
		.pause(1)
		.exec(http("IProcess request 2")
			.get("/api/iprocess/${processId}")
			.check(jsonPath("$.status").is("OK"))
		)
		.pause(1)
		.exec(http("IProcess request 3")
			.put("/api/iprocess/${processId}")
			.body(StringBody("""{"standard_input": "exit"}""")).asJSON
			.check(jsonPath("$.status").is("OK"))
		)
		.pause(6)
		.exec(http("IProcess request 4")
			.get("/api/iprocess/${processId}")
			.check(jsonPath("$.status").is("OK"), jsonPath("$.finished").ofType[Boolean].is(true))
		)
}

object JobSequence {
	val job =
		exec(http("Job request 1")
			.post("/api/jobs")
			.body(StringBody("""{"host": "zeus.cyfronet.pl", "script": "#!/bin/bash\necho hello\nexit 0"}""")).asJSON
			.check(jsonPath("$.status").saveAs("status"), jsonPath("$.job_id").saveAs("jobId"))
		)
		//printing the session to see the 'processId' value for debugging
		.exec {session =>
			println(session)
			
			session
		}
		.pause(1)
		.asLongAs(session => !session("status").as[String].equals("FINISHED")) {
			exec(http("Job request 2")
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
	val baseUrl = "https://submit.plgrid.pl"
	
	val httpProtocol = http
		.baseURL(baseUrl)
		.header("PROXY", "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURmekNDQW1lZ0F3SUJBZ0lJRDB2Q3JoZnRqV3d3RFFZSktvWklodmNOQVFFRkJRQXdkekVMTUFrR0ExVUUKQmhNQ1VFd3hFREFPQmdOVkJBb1RCMUJNTFVkeWFXUXhFekFSQmdOVkJBb1RDbFY2ZVhScmIzZHVhV3N4RVRBUApCZ05WQkFvVENFTlpSbEpQVGtWVU1SZ3dGZ1lEVlFRREV3OUVZVzVwWld3Z1NHRnlaWHBzWVdzeEZEQVNCZ05WCkJBTVRDM0JzWjJoaGNtVjZiR0ZyTUI0WERURTBNVEF4TXpBM05UQTBPVm9YRFRFME1UQXlNREEzTlRVME9Wb3cKZ1ljeEN6QUpCZ05WQkFZVEFsQk1NUkF3RGdZRFZRUUtFd2RRVEMxSGNtbGtNUk13RVFZRFZRUUtFd3BWZW5sMAphMjkzYm1sck1SRXdEd1lEVlFRS0V3aERXVVpTVDA1RlZERVlNQllHQTFVRUF4TVBSR0Z1YVdWc0lFaGhjbVY2CmJHRnJNUlF3RWdZRFZRUURFd3R3Ykdkb1lYSmxlbXhoYXpFT01Bd0dBMVVFQXhNRmNISnZlSGt3Z2dFaU1BMEcKQ1NxR1NJYjNEUUVCQVFVQUE0SUJEd0F3Z2dFS0FvSUJBUUNwYzZzbG1OZEtKR0hHaHNGcFhPU2E1ak9BYWJ6cwpUeDBXaVM2N3NzU2pxNktWOWlkQnVrRmVUTW5uTG1oMWs3THh6YjU1THdkTzlFYW1rYXNnMVQvaGtNUWh1OHFtCmYzZjUvdUZkKzEwQ2IyanMva210R3ptbWtMcVlVb0N6anBJclVYL0k1WHVOZFR6cDBJaWdyNWZrbi92NzdTSVkKYkliTlNpNkJrVEJCeXd3VUoraWJic1JDS01tVXlWYkFTSnJVRXhNc2VBamY3a0c2N1loWThDOTFlL2d1SmZSYQpqdkRScXdsUUtJRDhTdHQ1bC9HUURzK1k0Y3QrSGRqMHpSS3RhWERIRXplQ01SelFlZjd3VXFyVWx4OVEwMVMzCnl4cjZHakpkMThLNlBaWHRpeGx4MXIycEgrU3ppdG4yd3lsS0hTSC9hZ2hsSGgwK1d6ZWQrRnpsQWdNQkFBRXcKRFFZSktvWklodmNOQVFFRkJRQURnZ0VCQUNpd1FjYnVsNkt4dEQzMkJGT1l5dit6UjdLc0VUb2FkeU8zbUFMYgo0R2t1b0pNZEVHc2Z2NTY3M3hla2xRR1ZiS0ZJNVVXZzVJTEJXRmJEVEl0MWo4OVM5YmNpNjZWbDJUR25adVZCClF1OTB5bDlnTzBvRjRRYmlzRGpjWVFEbEhST1pYZVhVb1c0eUlTUzBqOTNyeTVBSGhCa1JWL09ER2p4cEFhMSsKSTlrWEkrbDg2OS9WTmR5VkZHUWtCc1dwN2gvMExTRnYvWU9IYWN4dEYva1lvSkErczhUTjEzbzQ2S1c5T0pRUwova2JRZWU5V05zczJSaTZ4MWJkWDJHY3pIZEFEMHlScHlDRG0vd3QxcEQzVlFWR1V5UFpwMG1BUjJabm9hNU16CkNDRUFraGhEY0p2ODdXOXdpWEVhYkpIUVhHSklBNy94WURMQTY2NjZvL2JrNklFPQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCi0tLS0tQkVHSU4gUlNBIFBSSVZBVEUgS0VZLS0tLS0KTUlJRW9nSUJBQUtDQVFFQXFYT3JKWmpYU2lSaHhvYkJhVnprbXVZemdHbTg3RThkRm9rdXU3TEVvNnVpbGZZbgpRYnBCWGt6SjV5NW9kWk95OGMyK2VTOEhUdlJHcHBHcklOVS80WkRFSWJ2S3BuOTMrZjdoWGZ0ZEFtOW83UDVKCnJSczVwcEM2bUZLQXM0NlNLMUYveU9WN2pYVTg2ZENJb0srWDVKLzcrKzBpR0d5R3pVb3VnWkV3UWNzTUZDZm8KbTI3RVFpakpsTWxXd0VpYTFCTVRMSGdJMys1QnV1MklXUEF2ZFh2NExpWDBXbzd3MGFzSlVDaUEvRXJiZVpmeAprQTdQbU9ITGZoM1k5TTBTcldsd3h4TTNnakVjMEhuKzhGS3ExSmNmVU5OVXQ4c2EraG95WGRmQ3VqMlY3WXNaCmNkYTlxUi9rczRyWjlzTXBTaDBoLzJvSVpSNGRQbHMzbmZoYzVRSURBUUFCQW9JQkFDTXg3eDVEL0pOZHN5RTAKNC84cVdDRUVKelpJd0FDay9mbXNUSlNYc05mOTBpc3JrVVBKbDhJcVdOVTNnbUxKWnFWcWdtRlJMMGRxM1RIRApsN09lRjBLV2V1WTJ4d1NGL1Rsamd4T3VIY2RmdEg5azRaQ2UwdjZSbVBBQ1V0RDRqOHVIaGU4SUd2MXFtRmhJCkE5aThIK1JRaGxueEN0L2Z5cS9nOUgvYzFCVVY3TWkzeUtoT1gzQTJBdkZiNGxxNjNFU0pVanB3OXNGOFl5Y2YKeCtwN3o3djY5MWxzWm5CRnlzbXNUUnpqOTFLdjhuZWZGTkpRanUySE53MzdxUkRlSmJZcUxyWjBtdVBEVU80cwpmUXNTalBLc1RrRzZqb05FbVJ2Z3ZLK0h2ejFLUi9jNEJFSmkxdjJwV0ZTS0lxNmV2TjJoUEVCOGpxLytRTmt6CnVDWWE1eEVDZ1lFQTAzZUFoMVhKaFliVXVRSVlyUHZ3Y21SQWVLYm1tbFljelg4VElwenFVNUV5MWYzRFduWk8KaXJjWVhvcDhuVGQxWG4vVmxZVDdoZDM2Vm9HNVRDcUlJTCtuaE53UDJUMktWOFNWbE9VK2lMQXEvVzFRdC9ZTgpKaEtQQ2ZTZ3BMUWNsb0Y0emN1TnFyU3gxRTNLZjVTOW1HVWl4YXdWT3VFV2gxTjlubk5XSk5zQ2dZRUF6U01UCk93Myt4VmMvaXlGK2tSNUxQTlc1ODc4T3FkalN5ODdlUUllRTRId0xZbG5YSWFvL0l6eUVqQTZML1RpZmh6YjMKS1QwQUtDT245Q3hwaUdxK3NMTk91enZTRVpxcnBYOTlsN2I3VHMzMlR1bHdHZzFKa245a3gvYkpLRStGeDMvdQprbmdKamxHaGUzeC92ei9FTStmeWs2a21vT1UweUJyR1p4c1VVVDhDZ1lBUXB5T0hCTmwyMjNSYkVYQXlCekViClFRdld0SVlLZDJlVFEwT1ZnVGZ1MXN0akVMWFdwT1J3aGhTME5VRzArODRSWUEzSTFZMGRNS3JyTHhJUmpuUGoKNkZQZjRaaVo2Zi9BQTZvbjdKbXRGL3A2dEtZYnBwUElnTU9rN0EvT2lLU0xzYXg2d0dIcXBJb3dodVBBZ2swZgorQzd4MUNoajE1LzNqanFoUk1IdTZ3S0JnSEdNNmRjMW53SXprbnY4VldnVTRjYlI4N1hGMjJNUEgxZ3JzWUQ4Cnd3UFVCaVV4MzlERndWOUszMy83aVdmbzBQMXk0b3VVTmFNQ1liVGFaS3hia2lTVGpKQTl3OEYwNFhDaHlxNlIKWGppMzc1Q3N6UjNPbnh3Vm9uN0J6NFdkeDBxd09Tb2dnZ0dpbEFXYjI4Yk1rRm0vR2RxOHRpSTMzZWlxVVlxLwpSWmJYQW9HQVFUY25zbGV2UFJCZTBuTSsvOXN2UENzWEVCTlpxem53NXRpdHFmUzV6Uk9vRHAvenM0V3RiYStLCnJWaks4R1pCOFVScHBvaUFoZUZveDJFRXpoTk9XVGpPUlBmQ1ZraEtmZTIvL3J1aDdzWitzbyt4U1lQYXBmRUkKQVQ2SWsxUG92ckVtc21ENHVUM0t1bUNjSVpPWk1jcE9aYm9VZUVqY2RrS2cyeVJTVm04PQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQotLS0tLUJFR0lOIENFUlRJRklDQVRFLS0tLS0KTUlJRStEQ0NBdUNnQXdJQkFnSUlEMHZDcmhmdGpXd3dEUVlKS29aSWh2Y05BUUVGQlFBd016RUxNQWtHQTFVRQpCaE1DVUV3eEVEQU9CZ05WQkFvVEIxQk1MVWR5YVdReEVqQVFCZ05WQkFNVENWTnBiWEJzWlNCRFFUQWVGdzB4Ck5EQTNNVGN4TVRFNE5ETmFGdzB4TlRBM01UY3hNVEU0TkROYU1IY3hDekFKQmdOVkJBWVRBbEJNTVJBd0RnWUQKVlFRS0V3ZFFUQzFIY21sa01STXdFUVlEVlFRS0V3cFZlbmwwYTI5M2JtbHJNUkV3RHdZRFZRUUtFd2hEV1VaUwpUMDVGVkRFWU1CWUdBMVVFQXhNUFJHRnVhV1ZzSUVoaGNtVjZiR0ZyTVJRd0VnWURWUVFERXd0d2JHZG9ZWEpsCmVteGhhekNDQVNJd0RRWUpLb1pJaHZjTkFRRUJCUUFEZ2dFUEFEQ0NBUW9DZ2dFQkFKRU5hOSt2Q01RRVV2NGUKT1VhQjgzSEdDQ2R2bThFRGNNMFUxU2tTZWVCN2MxSENxUzJ5dFV2SitHbmtNc1RKWHYzRVNyNUo2WW9scDMzegpMZWVZUFpFTm5MRHZ2UE9VWW8vNHdMSnNRRGw3SllrNDNscTUzRUlWd0hWOVVxa280UFVEUWZNelBWVkNrRk4vCnltK2dRQjl2clFobG9LdmNQUzJPdHRyV1QyMTFvaTJqVVdDQ3FTYXpSZU1lUHFHMzQ4S0dRRDUyTW50WnJXRVcKdXBTU3Bqakx6aU54aXBaVGNpSkpJcUdxcFVnS21jNjF4VkRWd3c0ZEJ0bWROb1ZaamZaeURSQXhSWithYUk0MwprUFExYUp6VlBpUHkwWHRSY2h3L0hFb2VLZ3VDQzVQQVg4WVluZjY0UDZDSlQ3ZlhMQ21nclkrMElKT1hvTkhiCjhQY3Q2Z1VDQXdFQUFhT0J5ekNCeURBZEJnTlZIUTRFRmdRVWlvdklQblVGTi8xRTZNYS92M2QwclMyMU9OSXcKREFZRFZSMFRBUUgvQkFJd0FEQWFCZ05WSFNBRUV6QVJNQThHRFNzR0FRUUJncFl0QVFFQkFRSXdOd1lEVlIwZgpCREF3TGpBc29DcWdLSVltYUhSMGNEb3ZMM0JzWjNKcFpDMXpZMkV1ZDJOemN5NTNjbTlqTG5Cc0wyTnliQzVrClpYSXdId1lEVlIwakJCZ3dGb0FVcVJOUldXdEVTMytjb2ZjNXpOZ0IvRlJWUUMwd0V3WURWUjBsQkF3d0NnWUkKS3dZQkJRVUhBd0l3RGdZRFZSMFBBUUgvQkFRREFnU3dNQTBHQ1NxR1NJYjNEUUVCQlFVQUE0SUNBUUJ1RUd2bwpxSzhCeFJGZFFkYnF1OExkOUovcUFUbXdMclAzQVRBeFljZ3pJempkbkxKNUh4VEdQK2ZxRUtGUnRNWVZNSzlEClU0T0lLbVIrbFNETGtsK2N5dUhsUk1EL1l2UlE0ZnM1ek1naEdvRTlmNjd0V1k2YmlHbThLLzJBWU95dHpJT2gKL28yNG1DYkR1OVVqZ1YvZ3hld3lVc01KNEZhY1FOdmsxMVp5L0xaalppb0E4VVhFSk5DM2d5QUZEdm94QWtSNQpyZVhqeURxY0o4UjVqKzBqRXpCQ0ZHbitFZmp1eWpwbkhJVG9DbkZQSVNHT1lpMlAyQ0Y0elpheVBPU2dRbWZiCkhicE96SkpXZC8rQjY4OWhPSVh4ZCtKODRGWFBGc3V5eG5ra0crODBkRjBvcStDK0FXNkYvcytQbDhjanRaM20KQTRHajltNnQwTHFXQytNR0VzS2ljRUVnNFNwRGs0cVNxd2hWaGtFVEdjTGhJTjhDVmFUaGVXUjlPNHNzbWhJcAo5S3B6dFNrY0ZhNVJCZXoxNVJsaDdJSzVOV0NJVkp0NXFVbTVvM1krRUx1SEJTMlpQeEo4MVJGd1VuaE9Qdm9mCkNvTmREUGF1cEpoSUNnVTZVdVlxYUloRVBmRjVzQW5FSkJIT0h5R3gvQitMTnVpSk9QaVFGdEorWjk3UnUxK2gKbjFXU0xZNHBaVFNLZDlRTFpKWHNTV3IrSzBEamJ3ZnlHUjlXTlI0bmQ1QTM1ZjBiU1k3UUpNZ05MQ3IzZm4xbwptTUhHVk1ZZmllSWxieEdmR0orVGNrYmpsS1NhUlo3MmlKK3lobUdSdnRUdmRLMmRWVm41SnpDVEQ1WTN1VFBzCnNsSTJpYjBBMnVYQmErZElUamJscVkrRGhNR2l6Y0c3dWw2Mm1RPT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=")
		.inferHtmlResources()
		.acceptHeader("*/*")
		.userAgentHeader("curl/7.37.1")

	val processScenario = scenario("Process scenario").exec(ProcessSequence.process)
			
	val iprocessScenario = scenario("IProcess scenario").exec(IProcessSequence.iprocess)
	
	val jobScenario = scenario("Job scenario").exec(JobSequence.job)

	setUp(
		processScenario.inject(atOnceUsers(1)),
		iprocessScenario.inject(atOnceUsers(1)),
		jobScenario.inject(atOnceUsers(1))
	).protocols(httpProtocol)
}