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
  def defaultTaxYear: Int
  def incomeTaxSubmissionBEBaseUrl: String
  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String
  def incomeTaxSubmissionStartUrl(taxYear: Int): String
  def incomeTaxSubmissionIvRedirect: String

  def incomeTaxEmploymentBEUrl: String
  def incomeTaxExpensesBEUrl: String
  def commonTaskListUrl(taxYear: Int): String

  def viewAndChangeEnterUtrUrl: String
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
  def emaSupportingAgentsEnabled: Boolean
}

@Singleton
class AppConfigImpl @Inject()(servicesConfig: ServicesConfig) extends AppConfig {
  private lazy val signInBaseUrl: String = servicesConfig.getString(ConfigKeys.signInUrl)

  private lazy val signInContinueBaseUrl: String = servicesConfig.getString(ConfigKeys.signInContinueUrl)
  lazy val signInContinueUrl: String = SafeRedirectUrl(signInContinueBaseUrl).encodedUrl //TODO add redirect to overview page
  private lazy val signInOrigin = servicesConfig.getString("appName")
  override lazy val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"

  override def defaultTaxYear: Int = servicesConfig.getInt(ConfigKeys.defaultTaxYear)

  override lazy val incomeTaxSubmissionBEBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionUrl) + "/income-tax-submission-service"

  def incomeTaxSubmissionBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontendUrl) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context")

  override def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")

  override def incomeTaxSubmissionStartUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    "/start"

  override def incomeTaxSubmissionIvRedirect: String = incomeTaxSubmissionBaseUrl +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.iv-redirect")

  override lazy val incomeTaxEmploymentBEUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxEmploymentUrl)}/income-tax-employment"

  override lazy val incomeTaxExpensesBEUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxExpensesUrl)}/income-tax-expenses"

  override def commonTaskListUrl(taxYear: Int): String = s"$incomeTaxSubmissionBaseUrl/$taxYear/tasklist"

  private lazy val vcBaseUrl: String = servicesConfig.getString(ConfigKeys.viewAndChangeUrl)

  override def viewAndChangeEnterUtrUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents/client-utr"

  lazy private val appUrl: String = servicesConfig.getString("microservice.url")
  lazy private val contactFrontEndUrl = servicesConfig.getString(ConfigKeys.contactFrontendUrl)

  lazy private val contactFormServiceIndividual = "update-and-submit-income-tax-return"
  lazy private val contactFormServiceAgent = "update-and-submit-income-tax-return-agent"

  private def contactFormServiceIdentifier(implicit isAgent: Boolean): String = if (isAgent) contactFormServiceAgent else contactFormServiceIndividual

  private def requestUri(implicit request: RequestHeader): String = SafeRedirectUrl(appUrl + request.uri).encodedUrl

  private lazy val feedbackFrontendUrl = servicesConfig.getString(ConfigKeys.feedbackFrontendUrl)

  override def feedbackSurveyUrl(implicit isAgent: Boolean): String = s"$feedbackFrontendUrl/feedback/$contactFormServiceIdentifier"

  override def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String =
    s"$contactFrontEndUrl/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=$requestUri"

  override def contactUrl(implicit isAgent: Boolean): String = s"$contactFrontEndUrl/contact/contact-hmrc?service=$contactFormServiceIdentifier"

  def getExcludedJourneysUrl(taxYear: Int, nino: String): String =
    s"$incomeTaxSubmissionBaseUrl/income-tax-submission-service/income-tax/nino/$nino/sources/excluded-journeys/$taxYear"

  private lazy val basGatewayUrl = servicesConfig.getString(ConfigKeys.basGatewayFrontendUrl)

  override lazy val signOutUrl: String = s"$basGatewayUrl/bas-gateway/sign-out-without-state"

  override lazy val timeoutDialogTimeout: Int = servicesConfig.getInt("timeoutDialogTimeout")
  override lazy val timeoutDialogCountdown: Int = servicesConfig.getInt("timeoutDialogCountdown")

  //Mongo config
  override lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  override lazy val mongoTTL: Int = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt

  override def taxYearErrorFeature: Boolean = servicesConfig.getBoolean("taxYearErrorFeatureSwitch")

  override def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override def routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  override lazy val welshToggleEnabled: Boolean = servicesConfig.getBoolean("feature-switch.welshToggleEnabled")
  override lazy val studentLoansEnabled: Boolean = servicesConfig.getBoolean("feature-switch.studentLoans")
  override lazy val taxableLumpSumsEnabled: Boolean = servicesConfig.getBoolean("feature-switch.taxableLumpSums")
  override lazy val employmentEOYEnabled: Boolean = servicesConfig.getBoolean("feature-switch.employmentEOYEnabled")
  override lazy val tailoringEnabled: Boolean = servicesConfig.getBoolean("feature-switch.tailoringEnabled")
  override lazy val useEncryption: Boolean = servicesConfig.getBoolean("useEncryption")
  override lazy val mimicEmploymentAPICalls: Boolean = servicesConfig.getBoolean("mimicEmploymentAPICalls")
  override lazy val offPayrollWorking: Boolean = servicesConfig.getBoolean("feature-switch.offPayrollWorking")
  override lazy val inYearDisabled: Boolean = servicesConfig.getBoolean("feature-switch.inYearDisabled")
  override lazy val sectionCompletedQuestionEnabled: Boolean = servicesConfig.getBoolean("feature-switch.sectionCompletedQuestionEnabled")
  override lazy val emaSupportingAgentsEnabled: Boolean = servicesConfig.getBoolean("feature-switch.ema-supporting-agents-enabled")
}
