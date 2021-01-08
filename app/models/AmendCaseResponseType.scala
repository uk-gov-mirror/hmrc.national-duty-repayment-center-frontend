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

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

sealed trait AmendCaseResponseType

object AmendCaseResponseType extends Enumerable.Implicits {

  case object Supportingdocuments extends WithName("supportingDocuments") with AmendCaseResponseType
  case object Furtherinformation extends WithName("furtherInformation") with AmendCaseResponseType

  val values: Seq[AmendCaseResponseType] = Seq(
    Supportingdocuments,
    Furtherinformation
  )

  def options(form: Form[_])(implicit messages: Messages): Seq[CheckboxItem] = values.map {
    value =>
      CheckboxItem(
        name = Some("value[]"),
        id = Some(value.toString),
        value = value.toString,
        content = Text(messages(s"amendCaseResponseType.${value.toString}")),
        checked = form("value").value.contains(value.toString)
      )
  }

  implicit val enumerable: Enumerable[AmendCaseResponseType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
