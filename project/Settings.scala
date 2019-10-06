
//Imports
import Common.seqBaseProjectTemplate
import Versions._
import sbt.Def

//Settings
object Settings {
  val serviceCoreSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(serviceCoreVersion)
  val usersSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(usersVersion)
  val walletsSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(walletsVersion)
  val transactionsSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(transactionsVersion)
  val businessRulesSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(businessRulesVersion)
  val programsSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(programsVersion)
  val couponsSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(couponsVersion)
  val pointsSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(pointsVersion)
}
