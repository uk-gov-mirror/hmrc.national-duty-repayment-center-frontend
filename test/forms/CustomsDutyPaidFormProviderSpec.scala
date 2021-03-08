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

import forms.behaviours.{DecimalFieldBehaviours, StringFieldBehaviours}
import models.CustomsDutyPaid
import play.api.data.{Form, FormError}

class CustomsDutyPaidFormProviderSpec extends DecimalFieldBehaviours with StringFieldBehaviours {

  val requiredKey = "customsDutyPaid.actualamountpaid.error.required"
  //val lengthKey = "customsDutyPaid.error.notANumber"
  val maxLength = 14
  val minimum = 0.00
  var maximum = 9999999.99

  val validDataGenerator = intsInRangeWithCommas(minimum.toInt, maximum.toInt)

  val form: Form[CustomsDutyPaid] = new CustomsDutyPaidFormProvider()()

  ".ActualPaidAmount" must {

    val fieldName = "ActualPaidAmount"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like decimalField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "customsDutyPaid.actualamountpaid.error.notANumber")
    )

    behave like decimalFieldWithMinimum(
      form,
      fieldName,
      0.01,
      expectedError = FormError(fieldName, "customsDutyPaid.actualamountpaid.error.greaterThanZero")

    )

    behave like decimalFieldWithMaximum(
      form,
      fieldName,
      maximum,
      expectedError = FormError(fieldName, "customsDutyPaid.actualamountpaid.error.length")
    )

    "not bind decimals with 3 decimal place" in {
      val result = form.bind(Map(fieldName -> "1.111"))(fieldName)
      result.errors shouldEqual Seq(
        FormError(fieldName, "customsDutyPaid.actualamountpaid.error.decimalPlaces", List(forms.Validation.monetaryPattern))
      )
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
