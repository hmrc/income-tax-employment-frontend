package services.employment

import support.builders.models.UserBuilder.aUser
import support.builders.models.details.EmploymentDetailsBuilder.anEmploymentDetails
import support.builders.models.mongo.EmploymentCYAModelBuilder.anEmploymentCYAModel
import support.builders.models.mongo.EmploymentUserDataBuilder.anEmploymentUserData
import support.builders.models.otheremployment.session.OtherEmploymentIncomeCYAModelBuilder.anOtherEmploymentIncomeCYAModel
import support.{TaxYearProvider, UnitTest}
import support.mocks.MockEmploymentSessionService

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class OtherEmploymentInfoServiceSpec extends UnitTest
  with TaxYearProvider
  with MockEmploymentSessionService {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val employmentId = "some-employment-id"

  private val underTest = new OtherEmploymentInfoService(mockEmploymentSessionService, ec)

  "updateLumpSum" should {
    "update the existing lump sum" in {
      val givenEmploymentUserData = anEmploymentUserData.copy(isPriorSubmission = false, hasPriorBenefits = false)
      val givenEmploymentUserDataWithLumpSum = givenEmploymentUserData.copy(employment = anEmploymentCYAModel(otherEmploymentIncome = Some(anOtherEmploymentIncomeCYAModel)))

      mockCreateOrUpdateUserDataWith(taxYearEOY, employmentId, givenEmploymentUserData.employment, Right(givenEmploymentUserData))

      await(underTest.updateLumpSums(aUser, taxYearEOY, employmentId, givenEmploymentUserData, anOtherEmploymentIncomeCYAModel.taxableLumpSums)) shouldBe
      Right(givenEmploymentUserDataWithLumpSum)
    }
  }
}
