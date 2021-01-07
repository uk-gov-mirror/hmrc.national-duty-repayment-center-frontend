package support

import config.FrontendAppConfig
import play.api.i18n.Lang
import play.api.mvc.Call

case class TestAppConfig(wireMockBaseUrl: String, wireMockPort: Int) extends FrontendAppConfig {

  override val appName: String = "trader-services-frontend"
  //override val baseInternalCallbackUrl: String = s"http://baseInternalCallbackUrl"
  //verride val baseExternalCallbackUrl: String = s"http://baseExternalCallbackUrl"
  override val authUrl: String = wireMockBaseUrl
  //override val traderServicesApiBaseUrl: String = wireMockBaseUrl
  override val upscanInitiateBaseUrl: String = wireMockBaseUrl

  //override val createCaseApiPath: String = "/create-case"
  //override val updateCaseApiPath: String = "/update-case"

  //override val mongoSessionExpiryTime: Int = 3600
  //override val authorisedStrideGroup: String = "TBC"

  //override val gtmContainerId: Option[String] = None
  override val contactHost: String = wireMockBaseUrl
  override val contactFormServiceIdentifier: String = "dummy"

  //override val exitSurveyUrl: String = wireMockBaseUrl + "/dummy-survey-url"
  //override val signOutUrl: String = wireMockBaseUrl + "/dummy-sign-out-url"
  //override val researchBannerUrl: String = wireMockBaseUrl + "dummy-research-banner-url"
  //override val subscriptionJourneyUrl: String = wireMockBaseUrl + "/dummy-subscription-url"

  //override val authorisedServiceName: String = "HMRC-XYZ"
  //override val authorisedIdentifierKey: String = "EORINumber"

  val fileFormats: FrontendAppConfig.FileFormats = FrontendAppConfig.FileFormats(10, "", "")

  override val timeout: Int = 10
  override val countdown: Int = 2

  override val analyticsToken: String = "an-token"
  override val analyticsHost: String = "an-host"
  override val reportAProblemPartialUrl: String = wireMockBaseUrl
  override val reportAProblemNonJSUrl: String = wireMockBaseUrl
  override val betaFeedbackUrl: String = wireMockBaseUrl
  override val betaFeedbackUnauthenticatedUrl: String = wireMockBaseUrl
  override val addressLookupServiceUrl: String = wireMockBaseUrl
  override val loginUrl: String = wireMockBaseUrl
  override val loginContinueUrl: String = wireMockBaseUrl
  override val languageTranslationEnabled: Boolean = true
  override val routeToSwitchLanguage: String => Call = (lang: String) => controllers.routes.LanguageController.switchToLanguage(lang)

  override def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

}
