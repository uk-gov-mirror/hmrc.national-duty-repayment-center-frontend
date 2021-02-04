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

import config.FrontendAppConfig
import connectors.{UpscanInitiateConnector, UpscanInitiateRequest}
import controllers.actions._
import forms.AdditionalFileUploadFormProvider
import javax.inject.Inject
import models.{ArticleType, ClaimantType, CustomsRegulationType, FileVerificationStatus, Mode, NormalMode, S3UploadError, UpscanNotification, UserAnswers}
import navigation.Navigator
import pages.{ArticleTypePage, BulkFileUploadPage, ClaimantTypePage, CustomsRegulationTypePage}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, RequestHeader, Result}
import play.mvc.Http.HeaderNames
import repositories.SessionRepository
import services.{FileUploadService, FileUploadState, FileUploaded, UploadFile, WaitingForFileVerification}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.{BulkFileUploadView, WaitingForFileVerificationView}

import scala.concurrent.{ExecutionContext, Future}

class BulkFileUploadController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        appConfig: FrontendAppConfig,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        upscanInitiateConnector: UpscanInitiateConnector,
                                        val controllerComponents: MessagesControllerComponents,
                                        waitingForFileVerificationView: WaitingForFileVerificationView,
                                        view: BulkFileUploadView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with FileUploadService {

  final val COOKIE_JSENABLED = "jsenabled"
  final val controller = routes.FileUploadController
  final val INITIAL_CALLBACK_WAIT_TIME_MILLIS = 2000
  type ConvertState = (FileUploadState) => Future[FileUploadState]
  case class SessionState(state: Option[FileUploadState], userAnswers: Option[UserAnswers])

  //GET /file-upload
  val showFileUpload: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    println("hey........................................")
    val state = request.userAnswers.fileUploadState
    for {
      fileUploadState <- initiateFileUpload(upscanRequest(request.internalId))(upscanInitiateConnector.initiate(_))(state)
      res <- updateSession(fileUploadState, Some(request.userAnswers))
      if (res)
    } yield renderState(request.userAnswers,fileUploadState)
  }

  private def updateSession(newState: FileUploadState, userAnswers: Option[UserAnswers]) = {
    if (userAnswers.nonEmpty)
      sessionRepository.set(userAnswers = userAnswers.get.copy(fileUploadState = Some(newState)))
    else Future.successful(true)
  }

  final def successRedirect(id: String)(implicit rh: RequestHeader) = appConfig.baseExternalCallbackUrl + (rh.cookies.get(COOKIE_JSENABLED) match {
    case Some(_) => controller.asyncWaitingForFileVerification(id)
    case None => controller.showWaitingForFileVerification
  })

  final def errorRedirect(id: String)(implicit rh: RequestHeader) =
    appConfig.baseExternalCallbackUrl + (rh.cookies.get(COOKIE_JSENABLED) match {
      case Some(_) => controller.asyncMarkFileUploadAsRejected(id)
      case None => controller.markFileUploadAsRejected
    })

  final def upscanRequest(id: String)(implicit rh: RequestHeader): UpscanInitiateRequest = {
    UpscanInitiateRequest(
      callbackUrl = appConfig.baseInternalCallbackUrl + controller.callbackFromUpscan(id).url,
      successRedirect = Some(successRedirect(id)),
      errorRedirect = Some(errorRedirect(id)),
      minimumFileSize = Some(1),
      maximumFileSize = Some(appConfig.fileFormats.maxFileSizeMb * 1024 * 1024),
      expectedContentType = Some(appConfig.fileFormats.approvedFileTypes)
    )
  }

  private def sessionState(id: String): Future[SessionState] = {
    for {
      u <- sessionRepository.get(id)
    } yield (SessionState(u.flatMap(_.fileUploadState), u))
  }

  final def renderState(userAnswers: UserAnswers, fileUploadState: FileUploadState, formWithErrors: Option[Form[_]] = None)(implicit request: Request[_]): Result = {
    fileUploadState match {
      case UploadFile(reference, uploadRequest, fileUploads, maybeUploadError) =>
        Ok(
          view(
            uploadRequest,
            fileUploads,
            maybeUploadError,
            successAction = getBulkEntryDetails(userAnswers),
            failureAction = controller.showFileUpload,
            checkStatusAction = controller.checkFileVerificationStatus(reference),
            backLink = routes.CustomsRegulationTypeController.onPageLoad(NormalMode)) //TODO: for more than one entry the back link should be diff. Make this method conditional when we get there
        )

      case WaitingForFileVerification(reference, _, _, _) =>
        Ok(
          waitingForFileVerificationView(
            successAction = getBulkEntryDetails(userAnswers),
            failureAction = controller.showFileUpload,
            checkStatusAction = controller.checkFileVerificationStatus(reference),
            backLink = controller.showFileUpload
          )
        )
    }
  }

  def or[T](formWithErrors: Option[Form[_]], emptyForm: Form[T], maybeFillWith: Option[T])(implicit request: Request[_]): Form[T] =
    formWithErrors
      .map(_.asInstanceOf[Form[T]])
      .getOrElse {
        if (request.flash.isEmpty) maybeFillWith.map(emptyForm.fill).getOrElse(emptyForm)
        else emptyForm.bind(request.flash.data)
      }

  val UpscanUploadErrorForm = Form[S3UploadError](
    mapping(
      "key" -> nonEmptyText,
      "errorCode" -> text,
      "errorMessage" -> text,
      "errorRequestId" -> optional(text),
      "errorResource" -> optional(text)
    )(S3UploadError.apply)(S3UploadError.unapply)
  )

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      println("bey........................................")
        val value = request.userAnswers.get(CustomsRegulationTypePage).get

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CustomsRegulationTypePage, value))
            updatedAnswersWithArticleType <- {
              updatedAnswers.get(CustomsRegulationTypePage) match {
                case Some(CustomsRegulationType.UKCustomsCodeRegulation) => Future.fromTry(updatedAnswers.set(ArticleTypePage, ArticleType.Schedule))
                case _ => Future.fromTry(request.userAnswers.set(CustomsRegulationTypePage, value))
              }
            }
            _              <- sessionRepository.set(updatedAnswersWithArticleType)
          } yield Redirect(navigator.nextPage(BulkFileUploadPage, mode, updatedAnswersWithArticleType))
  }

  private def getBulkEntryDetails(answers: UserAnswers): Call = answers.get(CustomsRegulationTypePage) match {
    case Some(CustomsRegulationType.UnionsCustomsCodeRegulation)  => routes.ArticleTypeController.onPageLoad(NormalMode)
    case _ => routes.EntryDetailsController.onPageLoad(NormalMode)
  }
}
