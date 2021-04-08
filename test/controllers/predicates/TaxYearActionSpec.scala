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

package controllers.predicates

import controllers.Assets.SEE_OTHER
import play.api.mvc.Result
import utils.UnitTestWithApp

import scala.concurrent.Future

class TaxYearActionSpec extends UnitTestWithApp {
  val validTaxYear: Int = 2022
  val invalidTaxYear: Int = 3000


  def taxYearAction(taxYear: Int): TaxYearAction = new TaxYearAction(taxYear)(mockAppConfig, mockMessagesControllerComponents)

  "TaxYearAction.refine" should {

    "return Right(request)" in {
      val result = await(taxYearAction(validTaxYear).refine(user))
      result shouldBe Right(user)
    }

    "return Left(request)" in {
      val result: Future[Result] = taxYearAction(invalidTaxYear).refine(user).map(_.left.get)
      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe "/income-through-software/return/employment-income/error/wrong-tax-year"
    }
  }

}
