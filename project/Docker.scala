
//Imports
import Common._
import Versions._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Def
import ModuleNames._

object Docker {

  val repo: Option[String] = Some("zeab")

  //Image List
  val I = new {
    val openjdk8Alpine: String = "openjdk:8-jdk-alpine"
    val openjdk8Slim: String = "openjdk:8-jdk-slim"
    val azulOpenjdk8Alpine: String = "azul/zulu-openjdk-alpine:8"
  }

  val usersDockerSettings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := I.azulOpenjdk8Alpine,
    dockerRepository := repo,
    dockerLabels := mapDockerLabels(usersKey, usersVersion, buildTime),
    dockerUpdateLatest := true
  )

  val walletsDockerSettings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := I.azulOpenjdk8Alpine,
    dockerRepository := repo,
    dockerLabels := mapDockerLabels(walletsKey, walletsVersion, buildTime),
    dockerUpdateLatest := true
  )

  val transactionsDockerSettings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := I.azulOpenjdk8Alpine,
    dockerRepository := repo,
    dockerLabels := mapDockerLabels(transactionsKey, transactionsVersion, buildTime),
    dockerUpdateLatest := true
  )

  val businessRulesDockerSettings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := I.azulOpenjdk8Alpine,
    dockerRepository := repo,
    dockerLabels := mapDockerLabels(businessRulesKey, businessRulesVersion, buildTime),
    dockerUpdateLatest := true
  )

}
