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

package support.mocks

import config.AppConfig
import org.scalamock.scalatest.MockFactory
import play.api.mvc.RequestHeader
import support.TaxYearProvider
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class MockAppConfig extends MockFactory with TaxYearProvider {

  def config(encrypt: Boolean = true,
             _mimicEmploymentAPICalls: Boolean = false,
             slEnabled: Boolean = true,
             isEmploymentEOYEnabled: Boolean = true,
             taxYearErrorEnabled: Boolean = true): AppConfig = new AppConfig(mock[ServicesConfig]) {

    override lazy val signInContinueUrl: String = "/continue"
    override lazy val signInUrl: String = "/signIn"

    override lazy val defaultTaxYear: Int = taxYear

    override def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = "/overview"

    override def incomeTaxSubmissionStartUrl(taxYear: Int): String = "/start"

    override def feedbackSurveyUrl(implicit isAgent: Boolean): String = "/feedbackUrl"

    override def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String = "/feedbackUrl"

    override def contactUrl(implicit isAgent: Boolean): String = "/contact-frontend/contact"

    override lazy val signOutUrl: String = "/sign-out-url"

    override lazy val timeoutDialogTimeout: Int = 900
    override lazy val timeoutDialogCountdown: Int = 120

    override lazy val taxYearErrorFeature: Boolean = taxYearErrorEnabled

    override lazy val welshToggleEnabled: Boolean = true

    override lazy val taxableLumpSumsEnabled: Boolean = true

    override lazy val tailoringEnabled: Boolean = false

    override lazy val nrsEnabled: Boolean = true

    override lazy val studentLoansEnabled: Boolean = slEnabled

    override lazy val employmentEOYEnabled: Boolean = isEmploymentEOYEnabled

    override def viewAndChangeEnterUtrUrl: String = "/report-quarterly/income-and-expenses/view/agents/client-utr"

    override def incomeTaxSubmissionBaseUrl: String = ""

    override def incomeTaxSubmissionIvRedirect: String = "/update-and-submit-income-tax-return/iv-uplift"

    override lazy val incomeTaxExpensesBEUrl: String = "http://localhost:11111/income-tax-expenses"
    override lazy val incomeTaxEmploymentBEUrl: String = "http://localhost:11111/income-tax-employment"
    override lazy val incomeTaxSubmissionBEBaseUrl: String = "http://localhost:11111/income-tax-submission-service"
    override lazy val nrsProxyBaseUrl: String = "http://localhost:11111"

    override lazy val encryptionKey: String = "encryptionKey12345"
    override lazy val useEncryption: Boolean = encrypt

    override lazy val mimicEmploymentAPICalls: Boolean = _mimicEmploymentAPICalls
  }
}
