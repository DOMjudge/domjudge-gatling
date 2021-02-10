package domjudge
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import User._
import Jury._

class AdminSetup extends Simulation {

  val httpProtocol = http
        .baseUrl(System.getProperty("baseurl"))
        .inferHtmlResources()

  val scn = scenario("AdminSetup")
        .exec(User.login("admin"))
        .exec(Jury.modify_config(Map(
              "config_registration_category_name" -> "Self-Registered",
              "config_compile_penalty" -> "on",
              "config_lazy_eval_results" -> "on",
              "config_show_flags" -> "on",
              "config_show_affiliations" -> "on",
              "config_show_teams_submissions" -> "on",
              "config_openid_autocreate_team" -> "on",
              )))
      //   .exec(Jury.create_contest("gatling","Gatling Test Contest"))
      //   .exec(Jury.enable_language("c#",      "csharp", List("csharp","cs")   ))
      //   .exec(Jury.enable_language("Ada",     "adb",    List("adb","ads")     ))
      //   .exec(Jury.enable_language("Fortran", "f95",    List("f95", "f90")    ))
      //   .exec(Jury.enable_language("Haskell", "hs",     List("hs", "lhs")     ))
      //   .exec(Jury.enable_language("Lua",     "lua",    List("lua")           ))
      //   .exec(Jury.enable_language("Pascal",  "pas",    List("pas", "p")      ))
      //   .exec(Jury.enable_language("Python2", "py2",    List("py2", "py")     ))
      //   .exec(Jury.enable_language("Python3", "py3",    List("py3")           ))
      //   .exec(Jury.enable_language("Ruby",    "rb",     List("rb")            ))
      //   .exec(Jury.enable_language("Scala",   "scala",  List("scala")         ))
      //   .exec(Jury.enable_language("Kotlin",  "kt",     List("kt")            ))
      //   .exec(Jury.upload_problem("hello-testcase.zip"))

setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
