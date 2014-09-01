package test

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class RecordedSimulation extends Simulation {

	val httpProtocol = http
		.baseURL("http://localhost:8080")
		.inferHtmlResources()
		.acceptHeader("""*/*""")
		.contentTypeHeader("""application/json""")
		.userAgentHeader("""curl/7.37.1""")

	val headers_0 = Map("""PROXY""" -> """based64proxy""")

    val uri1 = """localhost"""

	val scn = scenario("RecordedSimulation")
		.exec(http("request_0")
			.get("""/api/process""")
			.headers(headers_0)
			.body(RawFileBody("RecordedSimulation_request_0.txt")))

	setUp(
		//scn.inject(atOnceUsers(10))
		//scn.inject(constantUsersPerSec(10) during(30 seconds))
		scn.inject(atOnceUsers(20))
	).protocols(httpProtocol)
}
