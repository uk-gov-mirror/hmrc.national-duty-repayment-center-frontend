import sbt._

object AppDependencies {
  import play.core.PlayVersion

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "org.reactivemongo" %% "play2-reactivemongo"            % "0.18.6-play26",
    "uk.gov.hmrc"       %% "logback-json-logger"            % "4.6.0",
    "uk.gov.hmrc"       %% "govuk-template"                 % "5.60.0-play-26",
    "uk.gov.hmrc"       %% "play-health"                    % "3.16.0-play-26",
    "uk.gov.hmrc"       %% "play-ui"                        % "8.18.0-play-26",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"  % "1.2.0-play-26",
    "uk.gov.hmrc"       %% "bootstrap-play-26"              % "2.1.0",
    "uk.gov.hmrc"       %% "play-whitelist-filter"          % "3.1.0-play-26",
    "uk.gov.hmrc"       %% "play-frontend-govuk"            % "0.56.0-play-26",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"             % "0.26.0-play-26",
    "uk.gov.hmrc"       %% "auth-client"                    % "3.2.0-play-27"
  )

  val test = Seq(
    "org.pegdown"                 %  "pegdown"            % "1.6.0",
    "org.jsoup"                   %  "jsoup"              % "1.10.3",
    "com.typesafe.play"           %% "play-test"          % PlayVersion.current,
    "org.mockito"                 %  "mockito-all"        % "1.10.19",
    "org.scalacheck"              %% "scalacheck"         % "1.14.0"
  ).map(_ % Test)

  val itTest = Seq(
    "org.scalatest"          %% "scalatest"          % "3.2.3",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.27.2",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.36.8"
  ).map(_ % IntegrationTest)

  def apply(): Seq[ModuleID] = compile ++ test ++ itTest

  val akkaVersion = "2.5.23"
  val akkaHttpVersion = "10.0.15"

  val overrides = Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-protobuf" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
  )
}
