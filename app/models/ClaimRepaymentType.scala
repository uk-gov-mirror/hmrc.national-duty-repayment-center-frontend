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

package models

import viewmodels.RadioOption

sealed trait ClaimRepaymentType

object ClaimRepaymentType extends Enumerable.Implicits {

  case object Customs extends WithName("01") with ClaimRepaymentType
  case object Vat extends WithName("02") with ClaimRepaymentType
  case object Other extends WithName("03") with ClaimRepaymentType

  val values: Seq[ClaimRepaymentType] = Seq(
    Customs,
    Vat,
    Other
  )

  val options: Seq[RadioOption] = values.map {
    value =>
      RadioOption("claimRepaymentType", value.toString)
  }

  implicit val enumerable: Enumerable[ClaimRepaymentType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
