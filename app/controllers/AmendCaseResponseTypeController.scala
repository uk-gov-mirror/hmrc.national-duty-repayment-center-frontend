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
import forms.AmendCaseResponseTypeFormProvider
import models.AmendCaseResponseType.{FurtherInformation, SupportingDocuments}

import javax.inject.Inject
import models.{AmendCaseResponseType, Mode, UserAnswers}
import navigation.Navigator
import pages.{AmendCaseResponseTypePage, FurtherInformationPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.AmendCaseResponseTypeView

import scala.concurrent.{ExecutionContext, Future}

class AmendCaseResponseTypeController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: AmendCaseResponseTypeFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: AmendCaseResponseTypeView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  private def getBackLink(mode: Mode): Call = {
    routes.ReferenceNumberController.onPageLoad(mode)
  }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(AmendCaseResponseTypePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, getBackLink(mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, getBackLink(mode)))),

        value =>
          for {
            ua <- {
              if(!value.contains(FurtherInformation))
                Future.fromTry(request.userAnswers.remove(FurtherInformationPage))
              else if(!value.contains(SupportingDocuments))
                Future.successful(request.userAnswers.copy(fileUploadState = None))
              else Future.successful(request.userAnswers)
            }
            updatedAnswers <- Future.fromTry(ua.set(AmendCaseResponseTypePage, value))
            res   <- sessionRepository.set(updatedAnswers)
            if res
          } yield Redirect(navigator.nextPage(AmendCaseResponseTypePage, mode, updatedAnswers))
      )
  }
}
