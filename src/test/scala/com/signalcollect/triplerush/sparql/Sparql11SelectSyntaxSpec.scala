package com.signalcollect.triplerush.sparql

import java.io.{File, FileOutputStream}
import java.net.JarURLConnection
import java.nio.file.Files
import com.signalcollect.triplerush.TripleRush
import com.signalcollect.util.TestAnnouncements
import org.apache.commons.io.FileUtils
import org.apache.jena.query.QueryFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}
import scala.collection.JavaConversions.asScalaIterator

/**
 * Uses w3c test files to run SELECT syntax tests against Sparql 1.1 spec*
 */
  class Sparql11SelectSyntaxSpec extends FlatSpec with Matchers with BeforeAndAfter with TestAnnouncements {

  val tr = new TripleRush
  val graph = new TripleRushGraph(tr)
  implicit val model = graph.getModel
 // Unzip test jar into a temporary directory and delete after the tests are run.
  val tmpDir = Files.createTempDirectory("sparql-syntax")

  before {
    val url = getClass.getClassLoader.getResource("testcases-sparql-1.1-w3c/")
    val con = url.openConnection().asInstanceOf[JarURLConnection]
    val jarFile = con.getJarFile
    val entries = jarFile.entries()
    while (entries.hasMoreElements) {
      val entry = entries.nextElement()
      val f = new File(tmpDir + File.separator + entry.getName)
      if (entry.isDirectory) {
        f.mkdir()
      } else {
        val is = jarFile.getInputStream(entry)
        val fOutputStream = new FileOutputStream(f)
        while (is.available() > 0) {
          fOutputStream.write(is.read())
        }
        fOutputStream.close()
        is.close()
      }
    }
  }

 after {
    FileUtils.deleteDirectory(new File(tmpDir.toString))
    tr.shutdown()
  }

  "TripleRush" should "pass SELECT Sparql-1.1 syntax tests" in {
    val manifestFile = "testcases-sparql-1.1-w3c/manifest-all.ttl"
    //Load main manifest.
    tr.load(tmpDir.toString + File.separator + manifestFile)
    tr.awaitIdle
    tr.prepareExecution
    //Retrieve sub-manifests
    val subManifestQuery =
      """|PREFIX rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |PREFIX list:   <http://jena.hpl.hp.com/ARQ/list#>
        |PREFIX mf:     <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>
        |PREFIX qt:     <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>
        |SELECT DISTINCT ?subManifest
        |WHERE {
        |  <http://www.w3.org/TR/sparql11-query/> mf:conformanceRequirement ?list .
        |  ?list list:member ?subManifest .
        |}
        | """.stripMargin
    val subManifestsResultSet = Sparql(subManifestQuery)
    val subManifests = subManifestsResultSet.map(f => f.get("subManifest").toString).toList
    //Load sub-manifests.
    subManifests.map {
      subManifest =>
        val subManifestFile = subManifest.replace("file://", "")
        tr.load(subManifestFile)
        tr.awaitIdle
    }
    tr.prepareExecution
    //Retrieve location of query to run and type(whether it could parse or not).
    val query =
      """
        |PREFIX mf:  <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#>
        |PREFIX qt:  <http://www.w3.org/2001/sw/DataAccess/tests/test-query#>
        |PREFIX dawgt: <http://www.w3.org/2001/sw/DataAccess/tests/test-dawg#>
        |PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        |SELECT ?queryToRun ?type
        |WHERE { [] rdf:first ?testURI.
        |        ?testURI a ?type ;
        |        mf:action ?queryToRun ;
        |        dawgt:approval dawgt:Approved .
        |        FILTER(?type IN (mf:PositiveSyntaxTest11, mf:NegativeSyntaxTest11, mf:PositiveUpdateSyntaxTest11, mf:NegativeUpdateSyntaxTest11))
        |}
      """.stripMargin

    val results = Sparql(query)

    case class Test(queryToRun: String, positive: Boolean)

    val testsToRun = results.map(test => {
      val queryToRun = test.get("queryToRun").toString
      val typeOfTest = test.get("type").toString
      val positiveTest = typeOfTest.equals("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#PositiveSyntaxTest11") ||
        typeOfTest.equals("http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#PositiveUpdateSyntaxTest11")
      Test(queryToRun, positiveTest)
    }).toList

    var expectedNumOfPositiveTests = 0
    val expectedNumOfNegativeTests = testsToRun.count(p => !p.positive)
    var actualNumOfPositivePassed = 0
    var actualNumOfNegativePassed = 0
    testsToRun.map(test => {
      val query = scala.io.Source.fromFile(test.queryToRun.replace("file://", "")).mkString
      if (test.positive) {
        try {
          val queryFactoryQuery = QueryFactory.create(query)
          if (queryFactoryQuery.isSelectType && !query.contains("SERVICE")) {
            expectedNumOfPositiveTests += 1
            Sparql(query)
            actualNumOfPositivePassed += 1
          }
        } catch {
          case parseException: org.apache.jena.query.QueryParseException => // This is expected because QueryFactory.create works only
          // on QUERY and not on UPDATE, LOAD, INSERT etc.
          case illegalStateException: java.lang.IllegalStateException => //This one is expected as "SERVICE" isn't working or
          //even Jena probably doesn't work for this query.
        }
      }
      else {
        intercept[Exception] {
          actualNumOfNegativePassed += 1
          Sparql(query)
        }
      }
    })
    actualNumOfPositivePassed should be(expectedNumOfPositiveTests)
    actualNumOfNegativePassed should be(expectedNumOfNegativeTests)
  }

}