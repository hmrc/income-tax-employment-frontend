/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.UnitTest

class AppConfigSpec extends UnitTest {
  private val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  private val appUrl = "http://localhost:9308"
  private val appConfig = new AppConfig(mockServicesConfig)

  (mockServicesConfig.baseUrl(_: String)).expects("bas-gateway-frontend").returns("http://bas-gateway-frontend:9553")
  (mockServicesConfig.getConfString(_: String, _: String))
    .expects("bas-gateway-frontend.relativeUrl", *)
    .returns("http://bas-gateway-frontend:9553")

  (mockServicesConfig.baseUrl(_: String)).expects("feedback-frontend").returns("http://feedback-frontend:9553")
  (mockServicesConfig.getConfString(_: String, _: String))
    .expects("feedback-frontend.relativeUrl", *)
    .returns("http://feedback-frontend:9514")

  (mockServicesConfig.baseUrl(_: String)).expects("contact-frontend").returns("http://contact-frontend:9250")
  (mockServicesConfig.getConfString(_: String, _: String))
    .expects("contact-frontend.baseUrl", *)
    .returns("http://contact-frontend:9250")

  (mockServicesConfig.getString _).expects("microservice.url").returns(appUrl)

  "AppConfig" should {

    "return correct feedbackUrl" in {
      val expectedBackUrl = SafeRedirectUrl(appUrl + fakeRequest.uri).encodedUrl
      val expectedServiceIdentifier = "update-and-submit-income-tax-return"

      val expectedBetaFeedbackUrl =
        s"http://contact-frontend:9250/contact/beta-feedback?service=$expectedServiceIdentifier&backUrl=$expectedBackUrl"

      val expectedFeedbackSurveyUrl = s"http://feedback-frontend:9514/feedback/$expectedServiceIdentifier"
      val expectedContactUrl = s"http://contact-frontend:9250/contact/contact-hmrc?service=$expectedServiceIdentifier"
      val expectedSignOutUrl = s"http://bas-gateway-frontend:9553/bas-gateway/sign-out-without-state"

      appConfig.betaFeedbackUrl(fakeRequest) shouldBe expectedBetaFeedbackUrl

      appConfig.feedbackSurveyUrl shouldBe expectedFeedbackSurveyUrl

      appConfig.contactUrl shouldBe expectedContactUrl

      appConfig.signOutUrl shouldBe expectedSignOutUrl
    }
  }
}
