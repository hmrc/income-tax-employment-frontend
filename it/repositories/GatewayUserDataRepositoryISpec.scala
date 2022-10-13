package repositories

import models.AuthorisationRequest
import models.mongo.{EmploymentUserData, EncryptedEmploymentUserData, UserDataGateway}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import utils.IntegrationTest

import scala.concurrent.Future

class GatewayUserDataRepositoryISpec extends IntegrationTest with FutureAwaits with DefaultAwaitTimeout {

  private val gatewayRepo: GatewayUserDataRepositoryImpl = app.injector.instanceOf[GatewayUserDataRepositoryImpl]
  private def count: Long = await(gatewayRepo.collection.countDocuments().toFuture())

  class EmptyDatabase {
    await(gatewayRepo.collection.drop().toFuture())
    await(gatewayRepo.ensureIndexes)
    count mustBe 0
  }

  private def find(gatewayUserData: UserDataGateway)
                  (implicit authorisationRequest: AuthorisationRequest[_]): Future[Option[UserDataGateway]] = {
    gatewayRepo.collection
      .find(filter = Repository.filterGateway(authorisationRequest.user.sessionId, authorisationRequest.user.mtditid, authorisationRequest.user.nino))
      .toFuture()
      .map(_.headOption)
  }

  "createOrUpdate" should {
    "create a document in collection when one does not exist" in new EmptyDatabase {

    }

    "update a document in collection when one already exists" in new EmptyDatabase {
    }
  }

  "find" should {
      "get a document and update the TTL" in new EmptyDatabase {
      }
    }

}
