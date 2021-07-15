import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % "5.7.0",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "0.83.0-play-28",
    "uk.gov.hmrc"             %% "govuk-template"             % "5.69.0-play-28",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.50.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"  % "2.12.2"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "5.7.0"                 % Test,
    "org.scalatest"           %% "scalatest"                % "3.2.9"                 % Test,
    "org.jsoup"               %  "jsoup"                    % "1.13.1"                % Test,
    "com.typesafe.play"       %% "play-test"                % current                 % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.1.0"                 % "test, it",
    "com.github.tomakehurst"  %  "wiremock-jre8"            % "2.28.0"                % "test, it",
    "org.scalamock"           %% "scalamock"                % "5.1.0"                 % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"  % "0.50.0"                % "test, it"
  )
}
