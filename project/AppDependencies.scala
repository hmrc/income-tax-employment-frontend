/*
 * Copyright 2022 HM Revenue & Customs
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

import sbt.*

object AppDependencies {

  private val bootstrapFrontendPlay30Version = "10.5.0"
  private val mongoPlayVersion = "2.12.0"

  val jacksonAndPlayExclusions: Seq[InclusionRule] = Seq(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
    ExclusionRule(organization = "com.fasterxml.jackson.module"),
    ExclusionRule(organization = "com.fasterxml.jackson.core:jackson-annotations"),
    ExclusionRule(organization = "com.typesafe.play")
  )

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-30" % bootstrapFrontendPlay30Version,
    "uk.gov.hmrc"                   %% "play-frontend-hmrc-play-30" % "12.29.0",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % mongoPlayVersion,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.21.0",
    "com.beachape"                  %% "enumeratum"                 % "1.9.4",
    "com.beachape"                  %% "enumeratum-play-json"       % "1.9.4" excludeAll (jacksonAndPlayExclusions *)
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % bootstrapFrontendPlay30Version,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"  % mongoPlayVersion,
    "org.scalatest"           %% "scalatest"                % "3.2.19",
    "org.scalatestplus"       %% "mockito-3-4"              % "3.2.10.0",
    "org.mockito"             %% "mockito-scala"            % "2.0.0",
    "org.jsoup"               %  "jsoup"                    % "1.22.1",
    "com.github.tomakehurst"  %  "wiremock-jre8-standalone" % "3.0.1",
    "org.scalamock"           %% "scalamock"                % "7.5.5",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.64.8",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"  % mongoPlayVersion
  ).map(_ % Test)
}
