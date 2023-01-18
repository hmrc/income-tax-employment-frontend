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

package forms.benefits.medical

import forms.{AmountForm, YesNoForm}
import play.api.data.FormError
import support.UnitTest

class MedicalFormsProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty

  private val underTest = new MedicalFormsProvider()

  ".beneficialLoansForm" should {
    "return a form that maps data when data is correct" in {
      underTest.beneficialLoansForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.beneficialLoansForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.beneficialLoans.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.beneficialLoansForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.beneficialLoans.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.beneficialLoansForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.beneficialLoans.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.beneficialLoansForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.beneficialLoans.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".beneficialLoansAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.beneficialLoansAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.beneficialLoansAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.beneficialLoansAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.beneficialLoansAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.beneficialLoansAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.beneficialLoansAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.beneficialLoansAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.beneficialLoansAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.beneficialLoansAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.beneficialLoansAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".childcareForm" should {
    "return a form that maps data when data is correct" in {
      underTest.childcareForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.childcareForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.childcare.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.childcareForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.childcare.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.childcareForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.childcare.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.childcareForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.childcare.error.individual"), Seq())
        )
      }
    }
  }

  ".childcareAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.childcareAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.childcareAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.childcareAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.childcareAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.incorrectFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.childcareAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.childcareAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.childcareAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.childcareAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.incorrectFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.childcareAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.childcareBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".educationalServicesForm" should {
    "return a form that maps data when data is correct" in {
      underTest.educationalServicesForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.educationalServicesForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.educationalServices.error.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.educationalServicesForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.educationalServices.error.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.educationalServicesForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.educationalServices.error.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.educationalServicesForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.educationalServices.error.individual"), Seq())
        )
      }
    }
  }

  ".educationalServicesAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.educationalServicesAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.educationalServicesAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.educationalServicesAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.educationalServicesAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.invalidFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.educationalServicesAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.educationalServicesAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.educationalServicesAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.educationalServicesAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.invalidFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.educationalServicesAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.educationalServicesBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }

  ".medicalDentalForm" should {
    "return a form that maps data when data is correct" in {
      underTest.medicalDentalForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.medicalDentalForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalBenefits.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.medicalDentalForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalBenefits.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.medicalDentalForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalBenefits.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.medicalDentalForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalBenefits.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".medicalDentalChildcareForm" should {
    "return a form that maps data when data is correct" in {
      underTest.medicalDentalChildcareForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.medicalDentalChildcareForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalChildcare.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.medicalDentalChildcareForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalChildcare.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.medicalDentalChildcareForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalChildcare.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.medicalDentalChildcareForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("benefits.medicalDentalChildcare.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".medicalOrAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.medicalOrAmountForm(isAgent = anyBoolean).bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.medicalOrAmountForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.medicalOrAmountForm(isAgent = true).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is wrongFormat" in {
        underTest.medicalOrAmountForm(isAgent = true).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.invalidFormat.agent"), Seq())
        )
      }

      "when isAgent is true and data is overMaximum" in {
        underTest.medicalOrAmountForm(isAgent = true).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.overMaximum.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.medicalOrAmountForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.medicalOrAmountForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is wrongFormat" in {
        underTest.medicalOrAmountForm(isAgent = false).bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.invalidFormat.individual"), Seq())
        )
      }

      "when isAgent is false and data is overMaximum" in {
        underTest.medicalOrAmountForm(isAgent = false).bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("benefits.medicalOrDentalBenefitsAmount.error.overMaximum.individual"), Seq())
        )
      }
    }
  }
}
