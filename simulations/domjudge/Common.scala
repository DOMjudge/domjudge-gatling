package domjudge

import io.gatling.core.structure.ChainBuilder
import io.gatling.core.session.Expression
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.util.Calendar
import java.text.SimpleDateFormat


// Call this like one of the following:
// exec(User.login(session => session("user").as[String]))
// exec(User.login("username"))
// Password is optional, can be passed in the same way however:
// exec(User.login(session => session("user").as[String]), session => session("pass").as[String]))
// exec(User.login("username", "password"))
object User {
  def login(user: Expression[String], pass: Expression[String] = null): ChainBuilder = {
		val realpass = pass match {	case a:Expression[String] => a;	case _ => user }

    return exec(
        http("Login page get csrf")
        .get("/login")
        .check(
          regex("""<input type="hidden" name="_csrf_token" value="([^"]*)">""")
          .find
          .saveAs("csrftoken")
        ))
      .exec(http("Login Request")
        .post("/login")
        .formParam("_username", session => user(session))
        .formParam("_password", session => realpass(session))
        .formParam("_csrf_token", "${csrftoken}")
      )
	}

  def register(user: Expression[String], pass: Expression[String] = null): ChainBuilder = {
    val realpass = pass match {	case a:Expression[String] => a;	case _ => user }

    return exec(
        http("Registration page get csrf")
        .get("/register")
        .check(
          regex("""<input type="hidden" id="user_registration__token" name="user_registration\[_token\]" value="([^"]*)" """)
          .find
          .saveAs("csrftoken")
        ))
      .exec(http("Register user")
        .post("/register")
        .formParam("user_registration[username]", session => user(session))
        .formParam("user_registration[name]", session => user(session)) // todo make this say "Gatling ###"
        .formParam("user_registration[teamName]", session => user(session))
        .formParam("user_registration[plainPassword][first]", session => realpass(session))
        .formParam("user_registration[plainPassword][second]", session => realpass(session))
        .formParam("user_registration[existingAffiliation]", "1")
        .formParam("user_registration[_token]", "${csrftoken}")
      )
  }
}

object Team {
  def _submit(langid: String, filename: String) =
    exec(
      http("Get submit solution form")
      .get("/team/submit")
      .check(
        regex("""<input type="hidden" id="submit_problem__token" name="submit_problem\[_token\]" value="([^"]*)"""").find
        .saveAs("csrftoken")
        ,
        // get and save the problem id for the "hello" problem
        regex("""<option value="([^"]*)">hello""")
        .saveAs("problem_id")
    ))
    .exec(http("Submit Solution ${langid}")
      .post("/team/submit")
      .formParam("submit_problem[_token]","${csrftoken}")
      .formParam("submit_problem[problem]","${problem_id}")
      .formParam("submit_problem[language]", langid)
      .formUpload("submit_problem[code][]", filename)
      .formParam("submit", ""))
  def _submit_with_entrypoint(langid: String, filename: String, entry_point: String) =
    exec(
      http("Get submit solution form")
      .get("/team/submit")
      .check(
        regex("""<input type="hidden" id="submit_problem__token" name="submit_problem\[_token\]" value="([^"]*)"""").find
        .saveAs("csrftoken")
        ,
        // get and save the problem id for the "hello" problem
        regex("""<option value="([^"]*)">hello""")
        .saveAs("problem_id")
    ))
    .exec(http("Submit Solution ${langid}")
      .post("/team/submit")
      .formParam("submit_problem[_token]","${csrftoken}")
      .formParam("submit_problem[problem]","${problem_id}")
      .formParam("submit_problem[language]", langid)
      .formParam("submit_problem[entry_point]", entry_point)
      .formUpload("submit_problem[code][]", filename)
      .formParam("submit", ""))

  val submit_java      = exec(_submit("java",    "test-hello.java"))
  val submit_c         = exec(_submit("c",       "test-hello.c"))
  val submit_hs        = exec(_submit("hs",      "test-hello.hs"))
  val submit_lua       = exec(_submit("lua",     "test-hello.lua"))
  val submit_js        = exec(_submit("js",      "test-hello.js"))
  val submit_csharp    = exec(_submit("csharp",  "test-hello.cs"))
  val submit_py2       = exec(_submit("py2",     "test-hello.py2"))
  val submit_py3       = exec(_submit("py3",     "test-hello.py3"))
  val submit_nonewline = exec(_submit("c",       "test-output-nonewline.c"))
  val submit_kt        = exec(_submit_with_entrypoint("kt",  "test-hello.kt", "Test_helloKt"))
  def requestclarification() =
    exec(
        http("Get request clarification form")
        .get("/team/clarifications/add")
        .check(
          regex("""<input type="hidden" id="team_clarification__token" name="team_clarification\[_token\]" value="([^"]*)"""")
          .saveAs("csrftoken")
          ,
          // get and save the clarification subject id for the "General issue" category
          regex("""<option value="([^"]*-general)">General""").find.saveAs("clarification_subject")
        ))
    .exec(http("Request Clarification")
      .post("/team/clarifications/add")
      .formParam("team_clarification[recipient]", "dummy")
      .formParam("team_clarification[subject]", "${clarification_subject}")
      .formParam("team_clarification[message]", "${user} needs help")
      .formParam("team_clarification[_token]", "${csrftoken}")
      .formParam("submit", "")
    )

  val teampage = exec(http("Team Page").get("/team/")
      .check(
        regex("""(?s)<td class="scoretn.*" title="${user}">.*<a data-ajax-modal href="/team/team/(.*?)">""").find.saveAs("team_id")
      ))
  val teamdetails = exec(http("Team Details").get("/team/team/${team_id}"))
  val teamscoreboard = exec(http("Team Scoreboard").get("/team/scoreboard"))
}

object Spectator {
  def getscoreboard = exec(http("Public Scoreboard Request")
      .get("/public/"))
    // A spectator will check the scoreboard every 30 seconds for a set number of minutes
  def monitor_scoreboard(minutes: Int) = repeat(minutes*2, "n") {
    exec(getscoreboard).pause(30)
  }
}

object Jury {

  // TODO: add jury members browsing around/answering clarifications/etc
  // def view_submissions = ...

}
