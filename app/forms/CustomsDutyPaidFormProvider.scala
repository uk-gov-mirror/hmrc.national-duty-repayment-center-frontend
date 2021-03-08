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

package forms

import javax.inject.Inject
import forms.mappings.Mappings
import models.CustomsDutyPaid
import play.api.data.Form
import play.api.data.Forms._

class CustomsDutyPaidFormProvider @Inject() extends Mappings  {

  def apply(): Form[CustomsDutyPaid] = Form(
    mapping(
      "ActualPaidAmount" -> decimal("customsDutyPaid.actualamountpaid.error.required",
        "customsDutyPaid.actualamountpaid.error.notANumber")
        .verifying(
          firstError(
            regexp(Validation.monetaryPattern, "customsDutyPaid.actualamountpaid.error.decimalPlaces"),
            greaterThanZero("customsDutyPaid.actualamountpaid.error.greaterThanZero"),
            maximumValue("99999999999.99", "customsDutyPaid.actualamountpaid.error.length")
          )
        ),
      "ShouldHavePaidAmount" -> decimal("customsDutyPaid.shouldhavepaid.error.required",
        "customsDutyPaid.shouldhavepaid.error.notANumber")
        .verifying(
          firstError(
            regexp(Validation.monetaryPattern, "customsDutyPaid.shouldhavepaid.error.decimalPlaces"),
            greaterThanZero("customsDutyPaid.shouldhavepaid.error.greaterThanZero"),
            maximumValue("99999999999.99", "customsDutyPaid.shouldhavepaid.error.length")
          )
        )
    )(CustomsDutyPaid.apply)(CustomsDutyPaid.unapply)
      .verifying("customsDutyPaid.amounts.error.same", duty => duty.dueAmount != 0)
      .verifying("customsDutyPaid.amounts.error.greater", duty => duty.dueAmount >= 0)
  )
}
