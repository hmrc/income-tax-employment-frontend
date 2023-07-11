
package support.mocks

import models.User
import models.mongo.EmploymentUserData
import models.otheremployment.session.TaxableLumpSum
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import services.employment.OtherEmploymentInfoService

import scala.concurrent.Future

trait MockOtherEmploymentInfoService extends MockFactory {

  val mockOtherEmploymentInfoService: OtherEmploymentInfoService = mock[OtherEmploymentInfoService]

  def mockUpdateLumpSums(user: User,
                         taxYear: Int,
                         employmentId: String,
                         originalEmploymentUserData: EmploymentUserData,
                         newLumSum: Seq[TaxableLumpSum],
                         result: Future[Either[Unit, EmploymentUserData]]): CallHandler5[User, Int, String, EmploymentUserData, Seq[TaxableLumpSum], Future[Either[Unit, EmploymentUserData]]] = {
    (mockOtherEmploymentInfoService.updateLumpSums(_: User, _: Int, _: String, _: EmploymentUserData, _: Seq[TaxableLumpSum]))
      .expects(user, taxYear, employmentId, originalEmploymentUserData, newLumSum)
      .returning(result)
  }

}
