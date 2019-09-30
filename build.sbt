
//Imports
import Settings._
import Dependencies._
import Docker._
import ModuleNames._

//Sbt Log Level
logLevel := Level.Info

//Add all the command alias's
CommandAlias.allCommandAlias

lazy val servicecore = (project in file(serviceCoreKey))
  .settings(serviceCoreSettings: _*)
  .settings(libraryDependencies ++= serviceCoreDependencies)

lazy val users = (project in file(usersKey))
  .settings(usersSettings: _*)
  .dependsOn(servicecore)
  .settings(usersDockerSettings)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AssemblyPlugin)

lazy val wallets = (project in file(walletsKey))
  .settings(walletsSettings: _*)
  .dependsOn(servicecore)
  .settings(walletsDockerSettings)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AssemblyPlugin)

lazy val transactions = (project in file(transactionsKey))
  .settings(transactionsSettings: _*)
  .dependsOn(servicecore)
  .settings(transactionsDockerSettings)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AssemblyPlugin)

lazy val businessrules = (project in file(businessRulesKey))
  .settings(businessRulesSettings: _*)
  .dependsOn(servicecore)
  .settings(businessRulesDockerSettings)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AssemblyPlugin)
