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

package controllers

import controllers.actions._
import forms.RepaymentTypeFormProvider
import javax.inject.Inject
import models.ClaimantType.Importer
import models.RepaymentType.CMA
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.{BankDetailsPage, ClaimantTypePage, RepaymentTypePage, WhomToPayPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.RepaymentTypeView

import scala.concurrent.{ExecutionContext, Future}

class RepaymentTypeController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: RepaymentTypeFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: RepaymentTypeView
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(RepaymentTypePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers: UserAnswers <- Future.fromTry(request.userAnswers.set(RepaymentTypePage, value))
            updatesAnswersWithWhomToPay <- {
              request.userAnswers.get(ClaimantTypePage) match {
                case Some(Importer) => Future.fromTry(updatedAnswers.set(WhomToPayPage, models.WhomToPay.Importer))
                case _ => Future.successful(updatedAnswers)
              }
            }
            updatedAnswersWithCMA <- {
              request.userAnswers.get(RepaymentTypePage) match {
                case Some(CMA) => Future.fromTry(updatesAnswersWithWhomToPay.remove(BankDetailsPage))
                case _ => Future.successful(updatesAnswersWithWhomToPay)
              }
            }
            _ <- sessionRepository.set(updatedAnswersWithCMA)

          } yield Redirect(navigator.nextPage(RepaymentTypePage, mode, updatedAnswersWithCMA))
      )
  }
}
