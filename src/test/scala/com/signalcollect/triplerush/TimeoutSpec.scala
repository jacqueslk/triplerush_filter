/*
 * Copyright (C) 2015 Cotiviti Labs (nexgen.admin@cotiviti.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.signalcollect.triplerush

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeoutException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.scalatest.Matchers
import org.scalatest.concurrent.Futures
import org.scalatest.fixture.FlatSpec
import org.scalatest.fixture.UnitFixture

class TimeoutSpec extends FlatSpec with UnitFixture with Matchers with Futures {

  "TripleRush" should "throw a timeout exception when a blocking operation fails to complete in time" in new TestStore {
    val iri = "http://abc"
    intercept[TimeoutException] {
      tr.addStringTriple(iri, iri, iri, Duration.Zero)
    }
  }

  it should "throw a `StoreShutDownBeforeCompletionException` when a blocking operation fails due to store shutdown" in new TestStore {
    val canShutdown = new CountDownLatch(1)
    val canReturnNext = new CountDownLatch(1)
    def customIterator = new Iterator[(String, String, String)] {
      def hasNext = {
        canShutdown.countDown
        canReturnNext.await
        false
      }
      def next = ???
    }
    val iri = "http://abc"
    val d = tr.dictionary // Instantiate lazy `tr` (instantiating from Future causes strange issues)
    Future {
      canShutdown.await
      tr.close
      canReturnNext.countDown
    }
    intercept[StoreShutDownBeforeCompletionException] {
      tr.addStringTriples(i = customIterator, timeout = Duration.Inf)
    }
  }

}
