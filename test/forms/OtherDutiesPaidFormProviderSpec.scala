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

import forms.behaviours.DecimalFieldBehaviours
import play.api.data.{Form, FormError}

class OtherDutiesPaidFormProviderSpec extends DecimalFieldBehaviours {

  val requiredKey = "otherDutiesPaid.error.required"
  val lengthKey = "otherDutiesPaid.error.notANumber"
  val maxLength = 14
  val minimum = 0.00
  var maximum = 99999999999.99

  val validDataGenerator = intsInRangeWithCommas(minimum.toInt, maximum.toInt)

  val form: Form[String] = new OtherDutiesPaidFormProvider()()

  ".value" must {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like decimalField(
      form,
      fieldName,
      nonNumericError  = FormError(fieldName, "otherDutiesPaid.error.notANumber")
    )

    "not bind decimals with 3 decimal place" in {
      val result = form.bind(Map(fieldName -> "1.111"))(fieldName)
      result.errors shouldEqual Seq(
        FormError(fieldName, "otherDutiesPaid.error.decimalPlaces", List(forms.Validation.monetaryPattern))
      )
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
