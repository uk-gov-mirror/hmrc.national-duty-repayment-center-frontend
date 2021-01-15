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

package admin

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer.Context
import javax.inject.Inject

class MetricsService @Inject()(kenshooMetrics: com.kenshoo.play.metrics.Metrics)  {
  private val registry: MetricRegistry = kenshooMetrics.defaultRegistry

  def startTimer(metric: Timer): Context = registry.timer(metric.path).time()
  def mark(metric: Meter): Unit = registry.meter(metric.path).mark()
  def inc(metric: Counter): Unit = registry.counter(metric.path).inc()
  def dec(metric: Counter): Unit = registry.counter(metric.path).dec()

  case class InflightRequestMonitor(timerContext: Context, openRequestCounter: Counter, statusCounter: Int => Counter) {
    private var ended: Boolean = false

    def end(): Unit = end(None)
    def end(statusCode: Int): Unit = end(Some(statusCode))

    private def end(statusCode: Option[Int]): Unit = {
      if (!ended) {
        dec(openRequestCounter)
        statusCode.foreach(sc => inc(statusCounter(sc)))
        timerContext.stop()
        ended = true
      } else {()}
    }
  }

  def beginRequest[A](monitor: RequestMonitor)(block : InflightRequestMonitor => A): A = {
    inc(monitor.openRequestCounter)
    inc(monitor.counter)
    val context = startTimer(monitor.timer)
    val inflightRequestMonitor = InflightRequestMonitor(context, monitor.openRequestCounter, monitor.statusCounter)
    try {
      block(inflightRequestMonitor)
    } finally {
      inflightRequestMonitor.end()
    }
  }
}

case class Timer(name:String) {val path = s"$name.timer"}
case class Meter(name:String) {val path = s"$name.rate"}
case class Counter(name:String) {val path = s"$name"}

case class RequestMonitor(name: String) {
  val path = s"$name.request"
  val timer: Timer = Timer(path)
  val counter: Counter = Counter(path)
  val openRequestCounter: Counter = Counter(s"$path.openRequests")
  def statusCounter(statusCode: Int): Counter = Counter(s"$path.responseStatus.$statusCode")
}

object Monitors {
  val claimSubmission: RequestMonitor = RequestMonitor("ClaimSubmission")
  val addressLookup: RequestMonitor = RequestMonitor("AddressLookup")
  val barsCheck: RequestMonitor = RequestMonitor("BarsCheck")
  val contactDetailsSubmission: RequestMonitor = RequestMonitor("ContactDetailsSubmission")
  val phaseOneEligibilityExtension: RequestMonitor = RequestMonitor("PhaseOneEligibilityExtension")
  val eligibilityCheck: RequestMonitor = RequestMonitor("EligibilityCheck")
  val claimAmount: RequestMonitor = RequestMonitor("ClaimAmount")
  val potentialClaimant: RequestMonitor = RequestMonitor("PotentialClaimant")
  val ivJourneyStatus: RequestMonitor = RequestMonitor("IdentiyVerificationJourneyStatusCall")
  val ivEvidenceStatus: RequestMonitor = RequestMonitor("IdentiyVerificationEvidenceStatusCall")
  val auth: RequestMonitor = RequestMonitor("AuthCall")
  def authOutcomes(outcome: String): Meter = Meter(s"AuthOutcomes.$outcome")
  def ivOutcomes(outcome:String): Meter = Meter(s"IVOutcomes.$outcome")
  val claimExists: RequestMonitor = RequestMonitor("ClaimExists")
  val claimExistsRate: Meter = Meter("ClaimExists")
  def staggerPeriodAdvisedCount(staggerPeriod : String): Counter = Counter("StaggerPeriodAdvised." + staggerPeriod.replace(".",":"))
}
