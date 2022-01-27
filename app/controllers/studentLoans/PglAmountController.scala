
package controllers.studentLoans

import config.{AppConfig, ErrorHandler}
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.FormUtils
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PglAmountController @Inject()(implicit val mcc: MessagesControllerComponents,
                                    authAction: AuthorisedAction,
                                    inYearAction: InYearAction,
                                    appConfig: AppConfig,
                                    errorHandler: ErrorHandler,
                                    ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  def show(taxYear: Int, employmentId: String): Action[AnyContent] = authAction.async { implicit user =>

    inYearAction.notInYear(taxYear) {

    }

  }

}
