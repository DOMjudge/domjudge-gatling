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
  var feeder = Iterator.from(0).map(i => Map("user" -> f"gatlinguser$i%04d"))
  val httpProtocol = http
    .baseURL(System.getProperty("baseurl"))
    .inferHtmlResources()

  // Scenario that just grabs the public scoreboard every 30 seconds, some number of times
  // This will run for 10 minutes per "user"
  val public_boards = scenario("PublicScoreboards").exec(Spectator.monitor_scoreboard(10))

  // Takes around 3 minutes to run(and consists of some 36ish requests)
  val scn = scenario("TeamExample")
    .feed(feeder)
    // Load the public scoreboard, then register/login
    .exec(Spectator.getscoreboard).pause(1)
    // Try a few times to get us registered/logged in, if it fails the user is done
    .tryMax(5) {
        exec(User.register(session => session("user").as[String]))
        .exec(User.login(session => session("user").as[String])).pause(2)
    }
    .exitHereIfFailed

    // Make a submission, view team information, check scoreboard
    .exec(Team.teampage).pause(5)
    .exec(Team.submit_java).pause(10)
    .exec(Team.teamscoreboard).pause(10)
    .exec(Team.teamdetails).pause(5)
    .exec(Team.teampage).pause(7)
    .exec(Team.teamscoreboard).pause(9)
    .exec(Team.teampage).pause(3)

    // Submit a clarification and then check it
    .exec(Team.requestclarification).pause(6)
    .exec(Team.teamclarifications).pause(12)

    // Make some submissions, and check the team page periodically
    .exec(Team.teampage).pause(3)
    .exec(Team.teamscoreboard).pause(3)
    .exec(Team.submit_c).pause(15)
    .exec(Team.teampage).pause(6)
    .exec(Team.teampage).pause(14)
    .exec(Team.submit_py2).pause(8)
    .exec(Team.submit_hs).pause(5)
    .exec(Team.teampage).pause(3)
    .exec(Team.teampage).pause(4)
    .exec(Team.teamscoreboard).pause(8)
    .exec(Team.teampage).pause(2)
    .exec(Team.submit_c).pause(11)
    .exec(Team.teampage).pause(16)
    .exec(Team.teampage).pause(5)
  setUp(scn.inject(
			//atOnceUsers(10) // start with 10 users
            //constantUsersPerSec(5) during (30 seconds) randomized  // gets to ~150 users in 30seconds
            constantUsersPerSec(18) during (60 seconds) randomized  // gets to ~1000(ish) users in 60seconds
			//rampUsers(500) over (60 seconds)
  )).protocols(httpProtocol).pauses(uniformPausesPlusOrMinusPercentage(25)) // adjust pauses by 25percent either way

}
