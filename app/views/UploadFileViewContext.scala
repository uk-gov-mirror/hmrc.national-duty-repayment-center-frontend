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

package views

import com.google.inject.Inject
import config.FrontendAppConfig
import models.{DuplicateFileUpload, FileTransmissionFailed, FileUploadError, FileVerificationFailed, S3UploadError, UpscanNotification}
import play.api.data.FormError

import javax.inject.Singleton

@Singleton
class UploadFileViewContext @Inject()(appConfig: FrontendAppConfig) {

  def toFormError(error: FileUploadError): FormError =
    error match {
      case FileTransmissionFailed(error) =>
        FormError("file", Seq(toMessageKey(error)), Seq(appConfig.fileFormats.maxFileSizeMb))

      case FileVerificationFailed(details) =>
        FormError("file", Seq(toMessageKey(details)))

      /*case DuplicateFileUpload(checksum, existingFileName, duplicateFileName) =>
        FormError("file", Seq("error.file-upload.duplicate"))*/
    }

  def toMessageKey(error: S3UploadError): String =
    error.errorCode match {
      case "400" | "InvalidArgument" => "error.file-upload.required"
      case "InternalError"           => "error.file-upload.try-again"
      case "EntityTooLarge"          => "error.file-upload.invalid-size-large"
      case "EntityTooSmall"          => "error.file-upload.invalid-size-small"
      case _                         => "error.file-upload.unknown"
    }

  def toMessageKey(details: UpscanNotification.FailureDetails): String =
    details.failureReason match {
      case UpscanNotification.QUARANTINE => "error.file-upload.quarantine"
      case UpscanNotification.REJECTED   => "error.file-upload.invalid-type"
      case UpscanNotification.UNKNOWN    => "error.file-upload.unknown"
    }
}
