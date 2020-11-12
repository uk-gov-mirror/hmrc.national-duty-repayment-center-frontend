/*
 * Copyright 2020 HM Revenue & Customs
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
import models.Address
import play.api.data.{Form, Forms}
import play.api.data.Forms.{mapping, optional}

class ImporterAddressFormProvider @Inject() extends Mappings {

  private val maxLineLength = 128
  private val maxCityLength = 64
  private val maxRegionLength = 64
  private val maxCCLength = 2

  def apply(): Form[Address] = Form(
    mapping(
      "AddressLine1" ->
        text("importerAddress.error.line1.required")
          .verifying(firstError(
            maxLength(maxLineLength, "importerAddress.error.line1.length")
          )),
      "AddressLine2" ->
        optional(Forms.text
          .verifying(firstError(
            maxLength(maxLineLength, "importerAddress.error.line2.length")
          ))),
      "City" ->
        text("importerAddress.error.city.required")
          .verifying(firstError(
            maxLength(maxCityLength, "importerAddress.error.city.length")
          )),
      "Region" ->
        text("importerAddress.error.region.required")
          .verifying(firstError(
            maxLength(maxRegionLength, "importerAddress.error.region.length")
          )),
      "CountryCode" ->
        text("importerAddress.error.countryCode.required")
          .verifying(firstError(
            maxLength(maxRegionLength, "importerAddress.error.region.length")
          )),
      "PostalCode" ->
        optional(Forms.text
          .verifying(firstError(
            maxLength(maxCCLength, "importerAddress.error.postcode.min.length")
          )))
    )(Address.apply)(importerAddress => Some((importerAddress.AddressLine1, importerAddress.AddressLine2, importerAddress.City, importerAddress.Region, importerAddress.CountryCode, importerAddress.PostalCode)))
  )
}
