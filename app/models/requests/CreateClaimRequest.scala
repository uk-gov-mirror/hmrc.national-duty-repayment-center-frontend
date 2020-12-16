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

package models.requests

import java.time.LocalDate

import models.WhomToPay.Importer
import models._
import pages._
import play.api.libs.json.{Json, OFormat}

final case class CreateClaimRequest(
                                     Content: Content
                                   )

object CreateClaimRequest {
  implicit val formats: OFormat[CreateClaimRequest] = Json.format[CreateClaimRequest]

  def buildValidClaimRequest(userAnswers: UserAnswers): Option[CreateClaimRequest] = {


    def getClaimDetails(userAnswers: UserAnswers): Option[ClaimDetails] = for {
      customRegulationType <- userAnswers.get(CustomsRegulationTypePage)
      claimedUnderArticle <- userAnswers.get(ArticleTypePage)
      claimant <- userAnswers.get(ClaimantTypePage)
      claimType <- userAnswers.get(NumberOfEntriesTypePage)
      noOfEntries <- Some(userAnswers.get(HowManyEntriesPage))
      entryDetails <- userAnswers.get(EntryDetailsPage)
      claimReason <- userAnswers.get(ClaimReasonTypePage)
      claimDescription <- userAnswers.get(ReasonForOverpaymentPage)
      payeeIndicator <- userAnswers.get(WhomToPayPage)
      paymentMethod <- userAnswers.get(RepaymentTypePage)
    } yield ClaimDetails(FormType("01"),
      customRegulationType,
      claimedUnderArticle,
      claimant,
      claimType,
      noOfEntries,
      entryDetails,
      claimReason,
      claimDescription,
      LocalDate.now(),
      LocalDate.now(),
      payeeIndicator,
      paymentMethod)

    //TODO: Business decision to never send the VRN. API schema should be changed to replect this so we can change the UserDetails model
    def getAgentUserDetails(userAnswers: UserAnswers): Option[UserDetails] = for {
      eori <- userAnswers.get(EnterAgentEORIPage)
      name <- userAnswers.get(AgentNameImporterPage)
      address <- userAnswers.get(AgentImporterAddressPage)
      telephone <- userAnswers.get(PhoneNumberPage)
      email <- userAnswers.get(EmailAddressPage)
    } yield UserDetails(
      None,
      eori,
      name,
      address,
      Some(telephone),
      Some(email)
    )

    //TODO: Business decision to never send the VRN. API schema should be changed to reflect this so we can change the UserDetails model
    def getImporterUserDetails(userAnswers: UserAnswers): Option[UserDetails] = for {
      name <- userAnswers.get(ImporterNamePage)
      address <- userAnswers.get(ImporterAddressPage)
    } yield {
      print("XXXXXXX getImporterUserDetails")
      val eori = userAnswers.get(ImporterEoriPage).getOrElse(EORI("GBPR"))
      val email = "test email" //TODO need to get email from ContactByEmailPage
      val telephone = userAnswers.get(PhoneNumberPage)
      UserDetails(
        None,
        eori,
        name,
        address,
        telephone,
        Some(email)
      )
    }

    def getBankDetails(userAnswers: UserAnswers): Option[AllBankDetails] = userAnswers.get(RepaymentTypePage) match {
      case Some(RepaymentType.BACS) =>
        for {
          bankDetails <- userAnswers.get(BankDetailsPage)
        } yield (userAnswers.get(ClaimantTypePage), userAnswers.get(WhomToPayPage)) match {
          case (Some(models.ClaimantType.Importer), None) | (Some(models.ClaimantType.Representative), Some(Importer)) =>
            println("XXXXXXXX getBankDetails");
            AllBankDetails(ImporterBankDetails = Some(bankDetails), AgentBankDetails = None)
          case _ =>
            print("XXXXXXX getBankDetails 2")
            AllBankDetails(ImporterBankDetails = None, AgentBankDetails = Some(bankDetails))
        }
      case _ => None
    }

    def cleanseBankDetails(bankDetails: BankDetails): BankDetails = bankDetails.copy(
      SortCode = bankDetails.sortCodeTrimmed,
      AccountNumber = bankDetails.accountNumberPadded
    )

    /*def getCustomsValues(userAnswers: UserAnswers): DutyTypeTaxList = {

      val selectedDuties: Option[Set[ClaimRepaymentType]] = userAnswers.get(ClaimRepaymentTypePage)

      val getCustomsDutyPaid: Option[String] = selectedDuties.contains("01") match {
        case true => userAnswers.get(CustomsDutyPaidPage)
        case _ => Some("0.0")
      }

      val getCustomsDutyDue: Option[String] = selectedDuties.contains("01") match {
        case true => userAnswers.get(CustomsDutyDueToHMRCPage)
        case _ => Some("0.0")
      }

      DutyTypeTaxList(ClaimRepaymentType.Customs, getCustomsDutyPaid.getOrElse("0.0"),getCustomsDutyDue.getOrElse("0.0"),"0")
    }*/

    /*def getVatValues(userAnswers: UserAnswers): DutyTypeTaxList = {

      val selectedDuties: Option[Set[ClaimRepaymentType]] = userAnswers.get(ClaimRepaymentTypePage)

      val getVatPaid: Option[String] = selectedDuties.contains("02") match {
        case true => userAnswers.get(VATDueToHMRCPage)
        case _ => Some("0.0")
      }

      val getVatDue: Option[String] = selectedDuties.contains("02") match {
        case true => userAnswers.get(VATPaidPage)
        case _ => Some("0.0")
      }

      DutyTypeTaxList(ClaimRepaymentType.Customs, getVatPaid.getOrElse("0.0"),getVatDue.getOrElse("0.0"),"0")
    }*/

    /*def getDutyTypeTaxDetails(answers: UserAnswers): Seq[DutyTypeTaxList] = {
      Seq(getCustomsValues(answers) :: getVatValues(answers) :: Nil)
    }*/


    //val selectedDuties: Option[Set[ClaimRepaymentType]] = userAnswers.get(ClaimRepaymentTypePage)





      /*val getVatPaid: Option[String] = selectedDuties.contains(Set("02")) match {
        case true => userAnswers.get(VATPaidPage)
        case _ => Some("0.0")
      }

      val getVatDue: Option[String] = selectedDuties.contains(Set("02")) match {
        case true => userAnswers.get(VATDueToHMRCPage)
        case _ => Some("0.0")
      }

      val getOtherDutiesPaid: Option[String] = selectedDuties.contains(Set("03")) match {
        case true => userAnswers.get(OtherDutiesPaidPage)
        case _ => Some("0.0")
      }

      val getOtherDutiesDue: Option[String] = selectedDuties.contains(Set("03")) match {
        case true => userAnswers.get(OtherDutiesDueToHMRCPage)
        case _ => Some("0.0")
      }*/

      /*for {
        customsDutyPaid <- getCustomsDutyPaid
        customsDutyDue <- getCustomsDutyDue
        vatPaid <- getVatPaid
        vatDue <- getVatDue
        otherDutiesPaid <- getOtherDutiesPaid
        otherDutiesDue <- getOtherDutiesDue
        //dutyClaimAmount <- Some(customsDutyPaid.toDouble) - customsDutyDue.toDouble
        //vatClaimAmount <- (vatPaid.toDouble - vatDue.toDouble).toString
        //otherDutiesClaimAmount <- (otherDutiesPaid.toDouble - otherDutiesDue.toDouble).toString
      } yield {
        Seq(
          DutyTypeTaxList(ClaimRepaymentType.Customs, customsDutyPaid.toString, customsDutyDue.toString, "0"),
          DutyTypeTaxList(ClaimRepaymentType.Vat, vatPaid.toString, vatDue.toString, "0"),
          DutyTypeTaxList(ClaimRepaymentType.Other, otherDutiesPaid.toString, otherDutiesDue.toString, "0")
        )
      }*/


      /*def getTypeTaxDetails(userAnswers: UserAnswers): DutyTypeTaxDetails = {
        DutyTypeTaxDetails(getDutyTypeTaxList(userAnswers))
      }*/

    def getDocumentList(): DocumentList = {
      DocumentList(EvidenceSupportingDocs.Other, None)
    }

    def getContent(userAnswers: UserAnswers): Option[Content] = for {
      claimDetails <- getClaimDetails(userAnswers)
      importerDetails <- getImporterUserDetails(userAnswers)
      bankDetails <- getBankDetails(userAnswers)
    } yield {
      println("XXXXXXXX ClaimRepaymentTypePage" + userAnswers.get(ClaimRepaymentTypePage))
      val documentList: DocumentList = getDocumentList()
      val agentDetails: Option[UserDetails] = userAnswers.get(ClaimantTypePage) match {
        case Some(models.ClaimantType.Importer) => None
        case _ => getAgentUserDetails(userAnswers)
      }

      val dutyTypeTaxDetails = DutyTypeTaxDetails(Seq(DutyTypeTaxList(ClaimRepaymentType.Customs, "0.0", "0.0", "0.0") ))
      //val dutyTypeTaxDetails =  getTypeTaxDetails(userAnswers)

      Content(
        claimDetails,
        agentDetails,
        importerDetails,
        Some(bankDetails),
        dutyTypeTaxDetails,
        Seq(documentList)
      )
    }


    for {
      content <- getContent(userAnswers)
    } yield CreateClaimRequest(
      content
    )
  }
}

