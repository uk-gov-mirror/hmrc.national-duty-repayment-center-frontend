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
import forms.CustomsRegulationTypeFormProvider

import javax.inject.Inject
import models.{CheckMode, Mode, NormalMode, NumberOfEntriesType, UserAnswers}
import navigation.Navigator
import pages.{ArticleTypePage, CustomsRegulationTypePage, NumberOfEntriesTypePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.CustomsRegulationTypeView

import scala.concurrent.{ExecutionContext, Future}

class CustomsRegulationTypeController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: CustomsRegulationTypeFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: CustomsRegulationTypeView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(CustomsRegulationTypePage) match {
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
              userAnswers <- Future.fromTry (request.userAnswers.set(CustomsRegulationTypePage, value))
              _ <- sessionRepository.set(userAnswers)
            } yield
                Redirect(navigator.nextPage(CustomsRegulationTypePage, mode, userAnswers))
      )
  }
}
