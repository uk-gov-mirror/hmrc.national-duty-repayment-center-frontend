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

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import config.FrontendAppConfig
import connectors.{UpscanInitiateConnector, UpscanInitiateRequest}
import controllers.FileUploadUtils._
import controllers.actions._
import forms.{AdditionalFileUploadFormProvider, UpscanS3ErrorFormProvider}
import models.FileType.SupportingEvidence
import models.{AmendCaseResponseType, Mode, NormalMode, UpscanNotification, UserAnswers}
import pages.AmendCaseResponseTypePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import repositories.SessionRepository
import services._
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import views.html.{AmendCaseSendInformationView, AmendCaseUploadAnotherFileView}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class AmendCaseSendInformationController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    identify: IdentifierAction,
                                                    getData: DataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    sessionRepository: SessionRepository,
                                                    additionalFileUploadFormProvider: AdditionalFileUploadFormProvider,
                                                    appConfig: FrontendAppConfig,
                                                    upscanInitiateConnector: UpscanInitiateConnector,
                                                    val fileUtils: FileUploadUtils,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    val upscanS3ErrorFormProvider: UpscanS3ErrorFormProvider,
                                                    @Named("check-state-actor") checkStateActor: ActorRef,
                                                    fileUploadView: AmendCaseSendInformationView,
                                                    fileUploadedView: AmendCaseUploadAnotherFileView
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with FileUploadService {

  final val controller = routes.AmendCaseSendInformationController
  val UpscanUploadErrorForm = upscanS3ErrorFormProvider()

  // GET /file-verification
  final def showWaitingForFileVerification(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val timeout = Timeout(10 seconds)
    sessionRepository.getFileUploadState(request.internalId).flatMap { ss =>
      ss.state match {
        case Some(s) =>
          (checkStateActor ? CheckState(request.internalId, LocalDateTime.now.plusSeconds(10), s)).mapTo[FileUploadState].flatMap {
            case _: FileUploaded => Future.successful(Redirect(routes.AmendCaseSendInformationController.showFileUploaded(mode)))
            case _: UploadFile => Future.successful(Redirect(routes.AmendCaseSendInformationController.showFileUpload(mode)))
            case _ => Future.successful(missingFileUploadState)
          }
        case _ => Future.successful(missingFileUploadState)
      }
    }
  }

  //GET /file-verification/:reference/status
  final def checkFileVerificationStatus(reference: String): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    renderFileVerificationStatus(reference, request.userAnswers.fileUploadState)
  }

  final def removeFileUploadByReference(reference: String, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    sessionRepository.getFileUploadState(request.internalId).flatMap { ss =>
      ss.state match {
        case Some(s) =>
          val removeState = FileUploaded(fileUploads = s.fileUploads.copy(files = filesInStateAccepted(s.fileUploads.files)))
          val sessionState = ss.copy(state = Some(removeState))
          fileUtils.applyTransition(removeFileUploadBy(reference)(upscanRequest(request.internalId, mode))(upscanInitiateConnector.initiate(_))(_), removeState, sessionState).map {
          case _@FileUploaded(_, _) => Redirect(controller.showFileUploaded(mode))
          case _@UploadFile(_, _, _, _) => Redirect(controller.showFileUpload(mode))
          case s@_ => renderState(fileUploadState = s, mode = mode)
        }
        case None => Future.successful(missingFileUploadState)
      }
    }
  }

  //GET /file-upload
  def showFileUpload(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      ss <- sessionRepository.getFileUploadState(request.internalId)
      s <- Future.successful(ss.userAnswers.flatMap(_.fileUploadState))
      fs <- initiateFileUpload(upscanRequest(request.internalId, mode), Some(SupportingEvidence))(upscanInitiateConnector.initiate(_))(s)
      b <- fs match {
        case f@UploadFile(_, _, _, _) => sessionRepository.updateSession(f.copy(maybeUploadError = None), ss.userAnswers)
        case _ => sessionRepository.updateSession(fs, ss.userAnswers)
      }
      if b
    } yield renderState(fs, mode = mode)
  }

  //GET /file-uploaded
  def showFileUploaded(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    for {
      ss <- sessionRepository.getFileUploadState(request.internalId)
      s <- Future.successful(ss.userAnswers.flatMap(_.fileUploadState))
      if s.nonEmpty
    } yield {
      val uploadAnotherFileChoiceForm = additionalFileUploadFormProvider.UploadAnotherFileChoiceForm(ss.userAnswers.flatMap(_.fileUploadState.map(f => filesInStateAccepted(f.fileUploads.files).size)).getOrElse(0))
      Ok(fileUploadedView(
        uploadAnotherFileChoiceForm,
        s.get.fileUploads,
        controller.submitUploadAnotherFileChoice(mode),
        controller.removeFileUploadByReference,
        mode
      ))
    }
  }

  // POST /file-uploaded
  final def submitUploadAnotherFileChoice(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    sessionRepository.getFileUploadState(request.internalId).flatMap { ss =>
      val uploadAnotherFileChoiceForm = additionalFileUploadFormProvider.UploadAnotherFileChoiceForm(ss.userAnswers.flatMap(_.fileUploadState.map(f => filesInStateAccepted(f.fileUploads.files).size)).getOrElse(0))

      uploadAnotherFileChoiceForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(fileUploadedView(
          formWithErrors,
          request.userAnswers.fileUploadState.get.fileUploads,
          controller.submitUploadAnotherFileChoice(mode),
          controller.removeFileUploadByReference,
          mode
        ))),
        value =>
          sessionRepository.getFileUploadState(request.internalId).flatMap { ss =>
            ss.state match {
              case Some(s) if value =>
                fileUtils.applyTransition(submitedUploadAnotherFileChoice(upscanRequest(request.internalId, mode), Some(SupportingEvidence))(upscanInitiateConnector.initiate(_))(_), s, ss)
                  .map(_ => Redirect(routes.AmendCaseSendInformationController.showFileUpload(mode)))
              case Some(_) => Future.successful(Redirect(getAmendCaseUploadAnotherFile(request.userAnswers, mode)))
              case None => Future.successful(missingFileUploadState)
            }
          }
      )
    }

  }

  private def getAmendCaseUploadAnotherFile(answers: UserAnswers, mode: Mode): Call = {
    if (hasFurtherInformation(answers))
      routes.FurtherInformationController.onPageLoad(mode)
    else
      routes.AmendCheckYourAnswersController.onPageLoad()
  }

  def hasFurtherInformation(userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AmendCaseResponseTypePage) match {
      case Some(s) => s.contains(AmendCaseResponseType.FurtherInformation)
      case _ => false
    }
  }

  // GET /file-rejected
  final def markFileUploadAsRejected(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    UpscanUploadErrorForm.bindFromRequest().fold(
      _ => Future.successful(BadRequest),
      s3Error =>
        sessionRepository.getFileUploadState(request.internalId).flatMap { ss =>
          ss.state match {
            case Some(s) => fileUtils.applyTransition(fileUploadWasRejected(s3Error)(_), s, ss).map(_ => Redirect(routes.AmendCaseSendInformationController.showFileUpload(mode)))
            case None => Future.successful(missingFileUploadState)
          }
        }
    )
  }

  // POST /ndrc/:id/callback-from-upscan
  final def callbackFromUpscan(id: String): Action[UpscanNotification] = Action.async(parse.json.map(_.as[UpscanNotification])) { implicit request =>
    sessionRepository.getFileUploadState(id).flatMap { ss =>
      ss.state match {
        case Some(s) => fileUtils.applyTransition(upscanCallbackArrived(request.body, SupportingEvidence)(_), s, ss).map(newState => acknowledgeFileUploadRedirect(newState))
        case None => Future.successful(missingFileUploadState)
      }
    }
  }

  final def upscanRequest(id: String, mode: Mode): UpscanInitiateRequest = {
    UpscanInitiateRequest(
      callbackUrl = appConfig.baseInternalCallbackUrl + controller.callbackFromUpscan(id).url,
      successRedirect = Some(appConfig.baseExternalCallbackUrl + controller.showWaitingForFileVerification(mode)),
      errorRedirect = Some(appConfig.baseExternalCallbackUrl + controller.markFileUploadAsRejected(mode)),
      minimumFileSize = Some(1),
      maximumFileSize = Some(appConfig.fileFormats.maxFileSizeMb * 1024 * 1024),
      expectedContentType = Some(appConfig.fileFormats.approvedFileTypes)
    )
  }

  final def renderState(fileUploadState: FileUploadState, formWithErrors: Option[Form[_]] = None, mode: Mode)(implicit request: Request[_]): Result = {
    fileUploadState match {
      case UploadFile(reference, uploadRequest, fileUploads, maybeUploadError) => {
        Ok(
          fileUploadView(
            uploadRequest,
            fileUploads,
            maybeUploadError,
            successAction = controller.showFileUploaded(mode),
            failureAction = controller.showFileUpload(mode),
            checkStatusAction = controller.checkFileVerificationStatus(reference))
        )
      }

      case FileUploaded(fileUploads, _) =>
        val uploadAnotherFileChoiceForm = additionalFileUploadFormProvider.UploadAnotherFileChoiceForm(filesInStateAccepted(fileUploads.files).size)
        Ok(fileUploadedView(
          formWithErrors.getOrElse(uploadAnotherFileChoiceForm),
          fileUploads,
          controller.submitUploadAnotherFileChoice(mode),
          controller.removeFileUploadByReference,
          mode
        ))
    }
  }
}