package controllers.employment

import actions.AuthorisedAction

import javax.inject.Inject
import views.html.employment.CannotUpdateEmploymentView
import config.{AppConfig, ErrorHandler}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{MessagesControllerComponents, Result}
import play.mvc.Results.ok
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper

import scala.concurrent.ExecutionContext

case class CannotUpdateEmploymentController @Inject()(pageView: CannotUpdateEmploymentView)
  (implicit cc: MessagesControllerComponents,
  ec: ExecutionContext,
  appConf: AppConfig,
  authAction: AuthorisedAction)
  extends FrontendController(cc) with I18nSupport with SessionHelper with Logging {

    def show(taxYear: Int): Result = Ok(pageView(taxYear))

  }
