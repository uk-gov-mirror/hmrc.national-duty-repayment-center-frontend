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

package models.responses


import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.responses.ApiError

import java.time.LocalDateTime

final case class ClientClaimSuccessResponse(
                                             correlationId: String,
                                             error: Option[ApiError] = None,
                                             result: Option[NDRCFileTransferResult] = None
                                           )


object ClientClaimSuccessResponse {
  implicit val format: OFormat[ClientClaimSuccessResponse] = Json.format[ClientClaimSuccessResponse]
}

case class NDRCFileTransferResult(
                                   caseId: String,
                                   generatedAt: LocalDateTime
                                 )

object NDRCFileTransferResult {
  implicit val formats: Format[NDRCFileTransferResult] =
    Json.format[NDRCFileTransferResult]
}

case class NDRCFileTransferResponse(
                                     correlationId: String,
                                     error: Option[ApiError] = None,
                                     result: Option[NDRCFileTransferResult] = None
                                   ) {
  def isSuccess: Boolean = error.isEmpty && result.isDefined
  def isDuplicate: Boolean = error.exists(_.errorCode == "409")
}