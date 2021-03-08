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

package models.requests

import models._
import pages.{AmendCaseResponseTypePage, FurtherInformationPage, ReferenceNumberPage}
import play.api.libs.json.{Json, OFormat}

final case class AmendClaimRequest(
                                     Content: AmendContent,
                                     uploadedFiles: Seq[UploadedFile]
                                  )

object AmendClaimRequest {
  implicit val formats: OFormat[AmendClaimRequest] = Json.format[AmendClaimRequest]

  def buildValidAmendRequest(userAnswers: UserAnswers): Option[AmendClaimRequest] = {

    def getFurtherInformation(userAnswers: UserAnswers) : Option[String] =
      userAnswers.get(AmendCaseResponseTypePage).get.contains(AmendCaseResponseType.Furtherinformation) match {
        case true => userAnswers.get(FurtherInformationPage)
        case _ => Some("Files Uploaded")
    }

    def getAmendCaseResponseType(userAnswers: UserAnswers) : Seq[AmendCaseResponseType] = {
      userAnswers.get(AmendCaseResponseTypePage).getOrElse(Nil).toSeq
    }


    def getContent(userAnswers: UserAnswers): Option[AmendContent] = for {
      referenceNumber <- userAnswers.get(ReferenceNumberPage)
      furtherInformation <- getFurtherInformation(userAnswers)
    } yield {
      AmendContent(
        referenceNumber,
        furtherInformation,
        getAmendCaseResponseType(userAnswers)
      )
    }

    for {
      content <- getContent(userAnswers)
    } yield
      AmendClaimRequest(
        content,
        if(!hasSupportingDocs(userAnswers)) Nil else userAnswers.fileUploadState.map(_.fileUploads.toUploadedFiles).getOrElse(Nil)

      )
  }
  def hasSupportingDocs(userAnswers: UserAnswers): Boolean  = {
    userAnswers.get(AmendCaseResponseTypePage) match {
      case Some(s) => s.contains(AmendCaseResponseType.Supportingdocuments)
      case _ => false
    }
  }
}

