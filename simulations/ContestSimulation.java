package domjudge;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.function.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class ContestSimulation extends Simulation {
    // A fixed password (see seeding/accounts.json)
    String PASSWORD="secret";

    // How many "actions" a team will take (actions are things like submitting, requesting clarification, viewing a scoreboard)
    int NUM_TEAM_ACTIONS = 5;

    // Maximum amount of time the scenario can run
    int MAX_MINUTES = 5;

    private ChainBuilder login() {
        return exec(
            http("Login page get csrf")
            .get("/login")
            .check(
                css("input[name='_csrf_token']", "value")
                .find()
                .saveAs("csrftoken")
            ))
        .exec(http("Login Request")
            .post("/login")
            .formParam("_username", "#{user}")
            .formParam("_password", PASSWORD)
            .formParam("_csrf_token", "#{csrftoken}")
        );
    }

    private ChainBuilder submit(String langid, String filename) {
        return exec(
            http("Get submit form csrf from team page")
            .get("/team/submit")
            .check(
                css("input[id='submit_problem__token']", "value").find()
                .saveAs("csrftoken"),
                regex("<option value=\"([^\"]*)\">hello").find()
                .saveAs("problem_id")
            )
        ).exec(
            http(String.format("Submit solution %s", langid))
            .post("/team/submit")
            .formParam("submit_problem[_token]","#{csrftoken}")
            .formParam("submit_problem[problem]","#{problem_id}")
            .formParam("submit_problem[language]", langid)
            .formUpload("submit_problem[code][]", filename)
            .formParam("submit", "")
        );
    }

    private ChainBuilder requestClarification() {
        return exec(
            http("Get request clarification form")
            .get("/team/clarifications/add")
            .check(
                css("input[id=team_clarification__token", "value").find()
                .saveAs("csrftoken"),
                // get and save the clarification subject id for the "General issue" category
                regex("<option value=\"([^\"]*-general)\">General").find()
                .saveAs("clarification_subject")
            )
        ).exec(
            http("Request Clarification")
            .post("/team/clarifications/add")
            .formParam("team_clarification[recipient]", "dummy")
            .formParam("team_clarification[subject]", "#{clarification_subject}")
            .formParam("team_clarification[message]", "#{user} needs help")
            .formParam("team_clarification[_token]", "#{csrftoken}")
            .formParam("submit", "")
        );
    }

    /*********************** Spectator Scenario ****************************************************************
     * Scenario that just grabs the public scoreboard every 30 seconds. The scoreboard page autorefreshes
     * every 30s, so this is a good approximation of their activity.
     */
    HttpRequestActionBuilder publicScoreboard = http("Public Scoreboard").get("/public");
    ChainBuilder monitorScoreboard = exec(poll().every(30).exec(publicScoreboard)).pause(Duration.ofMinutes(MAX_MINUTES));
    ScenarioBuilder spectatorScenario = scenario("SpectatorScenario").exec(monitorScoreboard);


    /*********************** Team Scenario *********************************************************************
     * This pretends to be a team, it polls the team page every 30s and randomly performs various actions
     */
    // Feeder for team accounts
    AtomicInteger counter = new AtomicInteger(1);
    Iterator<Map<String,Object>> feeder =
        Stream.generate((Supplier<Map<String,Object>>) () -> {
            String username = String.format("gatling%04d", counter.getAndIncrement());
            return Collections.singletonMap("user", username);
        }
    ).iterator();

    HttpRequestActionBuilder teamPage = http("Team Page").get("/team");
    ChainBuilder teamSubmitC = submit("c", "test-hello.c");
    ChainBuilder teamSubmitCpp = submit("cpp", "test-hello.c++");
    ChainBuilder teamSubmitJava = submit("java", "test-hello.java");
    ChainBuilder teamSubmitKotlin = submit("kt", "test-hello.kt");
    ChainBuilder teamSubmitPython = submit("py3", "test-hello.py3");
    ChainBuilder teamRequestClarification = requestClarification();
    ChainBuilder teamProblems = exec(http("Team Problemset").get("/team/problems"));
    ChainBuilder teamScoreboard = exec(http("Team Scoreboard").get("/team/scoreboard"));
    ScenarioBuilder teamScenario = scenario("TeamScenario")
        .feed(feeder)
        .exec(http("Homepage").get("/"))
        .exec(publicScoreboard) // Fetch the public scoreboard
        .pause(1,5)               // Brief delay
        .exec(login())            // Log in as the team

        // Mimic the behavior of the team page, it autorefreshes every 30s
        .exec(
            poll().pollerName("teampage")
            .every(30)
            .exec(teamPage)
        )

        // While we're polling the teampage, lets pretend to do some actions like a real team might
        .repeat(NUM_TEAM_ACTIONS, "i").on(
            exec(
                randomSwitch().on(
                    // 20% chance of making a submission
                    Choice.withWeight(20.0, exec(
                        uniformRandomSwitch().on(
                            exec(teamSubmitC),
                            exec(teamSubmitCpp),
                            exec(teamSubmitJava),
                            exec(teamSubmitKotlin),
                            exec(teamSubmitPython)
                        )
                    )),
                    // 10% of requesting clarification
                    Choice.withWeight(10.0, teamRequestClarification),
                    // 30% of loading problems page
                    Choice.withWeight(30.0, teamProblems),
                    // 40% of loading scoreboard
                    Choice.withWeight(40.0, teamScoreboard)
                )
            )
            // Sleep anywhere between 5s and 1 minute before doing another action
            .pause(5,60)
        )

        // Stop polling the teampage
        .exec(
            poll().pollerName("teampage").stop()
        )

    ;


    HttpProtocolBuilder httpProtocol = http
        .baseUrl(System.getProperty("baseurl"))
        .inferHtmlResources()   // load all dependent things like images/js/css/etc
        .nameInferredHtmlResourcesAfterPath()
    ;

    {
        setUp(
            // teamScenario.injectOpen(atOnceUsers(5)),
            teamScenario.injectClosed(
                incrementConcurrentUsers(10)  // Batches of this many users
                .times(10)                    // how many "levels" to test
                .eachLevelLasting(60)         // hold the number of users for this duration
                .separatedByRampsLasting(60)  // Take 60s between adding each user
                .startingFrom(10)             // Start with 10 users
            ),
            // spectatorScenario.injectOpen(atOnceUsers(10))
            spectatorScenario.injectClosed(
                incrementConcurrentUsers(10)  // Batches of this many spectators
                .times(10)                    // how many "levels" to test
                .eachLevelLasting(60)         // hold the number of users for this duration
                .separatedByRampsLasting(60)  // Take 60s between adding each user
                .startingFrom(10)             // Start with 10 users
            )
        )
        .maxDuration(Duration.ofMinutes(MAX_MINUTES))
        .protocols(httpProtocol);
    }
}
