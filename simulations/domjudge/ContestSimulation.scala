package domjudge
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.util.Random

import User._
import Team._
import Spectator._

class ContestSimulation extends Simulation {
  // Configure the base url, and other global options related to all requests
  val httpProtocol = http
    .baseUrl(System.getProperty("baseurl"))
     // This makes it load dependent resources: things like images, javascript, css, etc
    .inferHtmlResources()

  // Scenario that just grabs the public scoreboard every 30 seconds, some number of times
  // This will run for 10 minutes per "user". The scoreboard page autorefreshes every 30s,
  // so this is a good approximation of their activity.
  val spectatorScenario = scenario("PublicScoreboards").exec(Spectator.monitor_scoreboard(10))

  // Generate an infinite number of users, starting from gatling000000
  // var feeder = Iterator.from(0).map(i => Map("user" -> f"gatling$i%05d"))
  var feeder = tsv("accounts.tsv").eager.random

  // This scenario pretends to be a team. It takes around 3 minutes, and makes ~250 requests(including dependent resources)
  // It tries to be similar to what a team might do, load the team page, submit clarifications, submit problems, load the scoreboard, etc.
  val teamScenario = scenario("TeamExample")
    .feed(feeder)
    // Load the public scoreboard, then register/login
    .exec(Spectator.getscoreboard).pause(1)
    // Try a few times to get us registered/logged in, if it fails the user is done
    .tryMax(5) {
        exec(User.register(session => session("user").as[String], session => session("pass").as[String]))
        .exec(User.login(session => session("user").as[String], session => session("pass").as[String])).pause(2)
    }
    .exitHereIfFailed

    // Make a submission, view team information, check scoreboard
    .exec(Team.teampage).pause(5)
    .exec(Team.submit_java).pause(10)
    .exec(Team.teamscoreboard).pause(10)

    // Do some more team like stuff
    .exec(Team.teamdetails).pause(5)
    .exec(Team.teampage).pause(7)
    .exec(Team.teamscoreboard).pause(9)
    .exec(Team.teampage).pause(3)

    // Submit a clarification and then "check" it
    .exec(Team.requestclarification).pause(6)
    .exec(Team.teampage).pause(12)

    // Make some submissions, and check the team page periodically
    .exec(Team.teampage).pause(3)
    .exec(Team.teamscoreboard).pause(3)
    .exec(Team.submit_c).pause(15)
    .exec(Team.teampage).pause(6)
    .exec(Team.teampage).pause(14)
    .exec(Team.submit_py2).pause(8)
    .exec(Team.submit_hs).pause(5)
    .exec(Team.submit_kt).pause(5)
    .exec(Team.teampage).pause(3)
    .exec(Team.teampage).pause(4)
    .exec(Team.teamscoreboard).pause(8)
    .exec(Team.teampage).pause(2)
    .exec(Team.submit_nonewline).pause(11)
    .exec(Team.teampage).pause(16)
    .exec(Team.teampage).pause(5)

  setUp(teamScenario.inject(
      atOnceUsers(1) // start with 1 users
			// atOnceUsers(100) // start with 100 users
            //constantUsersPerSec(5) during (30 seconds) randomized  // gets to ~150 users in 30seconds
      //      constantUsersPerSec(18) during (60 seconds) randomized  // gets to ~1000(ish) users in 60seconds
			//rampUsers(500) over (60 seconds)

      // Use with the throttle commands below
      //constantUsersPerSec(30) during (45 seconds) // 1350 users?
  ), spectatorScenario.inject(atOnceUsers(1)))
    /*.throttle(
      jumpToRps(20).holdFor(2 minutes)
    )*/
    .maxDuration(6 minutes)
    .throttle(
      // reachRps(1) in (30 seconds),
      // reachRps(5) in (30 seconds),
      // reachRps(10) in (30 seconds),
      // reachRps(25) in (30 seconds),
      reachRps(50) in (30 seconds),
      holdFor(1 minute),
      reachRps(100) in (30 seconds),
      // holdFor(1 minute),
      // reachRps(150) in (30 seconds),
      // holdFor(1 minute),
      // reachRps(200) in (30 seconds),
      // holdFor(2 minutes)
      // reachRps(250) in (30 seconds),
      // holdFor(1 minute),
      // reachRps(300) in (30 seconds),
      // holdFor(1 minute),
      // reachRps(400) in (30 seconds),
      // holdFor(1 minute),
      // reachRps(500) in (30 seconds),
      // holdFor(5 minutes)
    )
    .protocols(httpProtocol)
    .pauses(uniformPausesPlusOrMinusPercentage(25)) // adjust pauses by 25percent either way

}
