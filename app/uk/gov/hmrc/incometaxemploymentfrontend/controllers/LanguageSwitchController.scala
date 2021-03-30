package uk.gov.hmrc.incometaxemploymentfrontend.controllers

import uk.gov.hmrc.incometaxemploymentfrontend.config.AppConfig
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}
import play.api.mvc._
import play.api.i18n.Lang
import com.google.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageSwitchController @Inject()(
  appConfig: AppConfig,
  languageUtils: LanguageUtils,
  cc: ControllerComponents)
    extends LanguageController(languageUtils, cc) {
  import appConfig._

  override def fallbackURL: String =
    "https://www.gov.uk/government/organisations/hm-revenue-customs"

  override protected def languageMap: Map[String, Lang] = {
    if (appConfig.welshLanguageSupportEnabled) Map(en -> Lang(en), cy -> Lang(cy))
    else Map(en -> Lang(en))
  }

}
