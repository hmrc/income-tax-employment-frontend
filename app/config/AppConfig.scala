/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import com.google.inject.ImplementedBy
import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {
  def signInUrl: String
  def vcSessionServiceBaseUrl: String
  def defaultTaxYear: Int
  def incomeTaxSubmissionBEBaseUrl: String
  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String
  def incomeTaxSubmissionStartUrl(taxYear: Int): String
  def incomeTaxSubmissionIvRedirect: String

  def incomeTaxEmploymentBEUrl: String
  def incomeTaxExpensesBEUrl: String
  def commonTaskListUrl(taxYear: Int): String

  def viewAndChangeEnterUtrUrl: String
  def viewAndChangeAgentsUrl: String
  def feedbackSurveyUrl(implicit isAgent: Boolean): String
  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String
  def contactUrl(implicit isAgent: Boolean): String

  def signOutUrl: String
  def timeoutDialogTimeout: Int
  def timeoutDialogCountdown: Int

  //Mongo config
  def encryptionKey: String
  def mongoTTL: Int

  def taxYearErrorFeature: Boolean
  def languageMap: Map[String, Lang]

  def routeToSwitchLanguage: String => Call

  def welshToggleEnabled: Boolean
  def studentLoansEnabled: Boolean
  def taxableLumpSumsEnabled: Boolean
  def employmentEOYEnabled: Boolean
  def tailoringEnabled: Boolean
  def useEncryption: Boolean
  def mimicEmploymentAPICalls: Boolean
  def offPayrollWorking: Boolean
  def inYearDisabled: Boolean
  def sectionCompletedQuestionEnabled: Boolean
}

@Singleton
class AppConfigImpl @Inject()(servicesConfig: ServicesConfig) extends AppConfig {
  private lazy val signInBaseUrl: String = servicesConfig.getString(ConfigKeys.signInUrl)

  private lazy val signInContinueBaseUrl: String = servicesConfig.getString(ConfigKeys.signInContinueUrl)
  lazy val signInContinueUrl: String = SafeRedirectUrl(signInContinueBaseUrl).encodedUrl //TODO add redirect to overview page
  private lazy val signInOrigin = servicesConfig.getString("appName")
  lazy val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"

  lazy val vcSessionServiceBaseUrl: String = servicesConfig.baseUrl("income-tax-session-data")
  def defaultTaxYear: Int = servicesConfig.getInt(ConfigKeys.defaultTaxYear)

  lazy val incomeTaxSubmissionBEBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionUrl) + "/income-tax-submission-service"

  def incomeTaxSubmissionBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontendUrl) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context")

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")

  def incomeTaxSubmissionStartUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    "/start"

  def incomeTaxSubmissionIvRedirect: String = incomeTaxSubmissionBaseUrl +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.iv-redirect")

  lazy val incomeTaxEmploymentBEUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxEmploymentUrl)}/income-tax-employment"

  lazy val incomeTaxExpensesBEUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxExpensesUrl)}/income-tax-expenses"

  def commonTaskListUrl(taxYear: Int): String = s"$incomeTaxSubmissionBaseUrl/$taxYear/tasklist"

  private lazy val vcBaseUrl: String = servicesConfig.getString(ConfigKeys.viewAndChangeUrl)

  def viewAndChangeEnterUtrUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents/client-utr"
  def viewAndChangeAgentsUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents"

  lazy private val appUrl: String = servicesConfig.getString("microservice.url")
  lazy private val contactFrontEndUrl = servicesConfig.getString(ConfigKeys.contactFrontendUrl)

  lazy private val contactFormServiceIndividual = "update-and-submit-income-tax-return"
  lazy private val contactFormServiceAgent = "update-and-submit-income-tax-return-agent"

  private def contactFormServiceIdentifier(implicit isAgent: Boolean): String = if (isAgent) contactFormServiceAgent else contactFormServiceIndividual

  private def requestUri(implicit request: RequestHeader): String = SafeRedirectUrl(appUrl + request.uri).encodedUrl

  private lazy val feedbackFrontendUrl = servicesConfig.getString(ConfigKeys.feedbackFrontendUrl)

  def feedbackSurveyUrl(implicit isAgent: Boolean): String = s"$feedbackFrontendUrl/feedback/$contactFormServiceIdentifier"

  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String =
    s"$contactFrontEndUrl/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=$requestUri"

  def contactUrl(implicit isAgent: Boolean): String = s"$contactFrontEndUrl/contact/contact-hmrc?service=$contactFormServiceIdentifier"

  def getExcludedJourneysUrl(taxYear: Int, nino: String): String =
    s"$incomeTaxSubmissionBaseUrl/income-tax-submission-service/income-tax/nino/$nino/sources/excluded-journeys/$taxYear"

  private lazy val basGatewayUrl = servicesConfig.getString(ConfigKeys.basGatewayFrontendUrl)

  lazy val signOutUrl: String = s"$basGatewayUrl/bas-gateway/sign-out-without-state"

  lazy val timeoutDialogTimeout: Int = servicesConfig.getInt("timeoutDialogTimeout")
  lazy val timeoutDialogCountdown: Int = servicesConfig.getInt("timeoutDialogCountdown")

  //Mongo config
  lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  lazy val mongoTTL: Int = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt

  def taxYearErrorFeature: Boolean = servicesConfig.getBoolean("taxYearErrorFeatureSwitch")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val welshToggleEnabled: Boolean = servicesConfig.getBoolean("feature-switch.welshToggleEnabled")
  lazy val studentLoansEnabled: Boolean = servicesConfig.getBoolean("feature-switch.studentLoans")
  lazy val taxableLumpSumsEnabled: Boolean = servicesConfig.getBoolean("feature-switch.taxableLumpSums")
  lazy val employmentEOYEnabled: Boolean = servicesConfig.getBoolean("feature-switch.employmentEOYEnabled")
  lazy val tailoringEnabled: Boolean = servicesConfig.getBoolean("feature-switch.tailoringEnabled")
  lazy val useEncryption: Boolean = servicesConfig.getBoolean("useEncryption")
  lazy val mimicEmploymentAPICalls: Boolean = servicesConfig.getBoolean("mimicEmploymentAPICalls")
  lazy val offPayrollWorking: Boolean = servicesConfig.getBoolean("feature-switch.offPayrollWorking")
  lazy val inYearDisabled: Boolean = servicesConfig.getBoolean("feature-switch.inYearDisabled")
  lazy val sectionCompletedQuestionEnabled: Boolean = servicesConfig.getBoolean("feature-switch.sectionCompletedQuestionEnabled")
}
