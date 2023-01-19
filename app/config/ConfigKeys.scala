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

object ConfigKeys {
  val incomeTaxSubmissionUrl = "microservice.services.income-tax-submission.url"

  val contactFrontendUrl = "microservice.services.contact-frontend.url"
  val incomeTaxSubmissionFrontendUrl = "microservice.services.income-tax-submission-frontend.url"
  val incomeTaxEmploymentUrl = "microservice.services.income-tax-employment.url"
  val incomeTaxExpensesUrl = "microservice.services.income-tax-expenses.url"
  val basGatewayFrontendUrl = "microservice.services.bas-gateway-frontend.url"
  val feedbackFrontendUrl = "microservice.services.feedback-frontend.url"
  val viewAndChangeUrl = "microservice.services.view-and-change.url"
  val signInUrl = "microservice.services.sign-in.url"
  val signInContinueUrl = "microservice.services.sign-in.continueUrl"
  val incomeTaxNrsProxyUrl = "microservice.services.income-tax-nrs-proxy.url"

  val defaultTaxYear = "defaultTaxYear"
}
