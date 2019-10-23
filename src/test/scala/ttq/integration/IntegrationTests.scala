package ttq.integration

import org.scalatest.Ignore
import org.scalatra.test.scalatest._
import ttq.web.HintServlet

// slow running, manually tested
@Ignore
class IntegrationTests extends ScalatraFunSuite {
  addServlet(classOf[HintServlet], "/*")

  test("simple test") {
    postJson("/",
      """{
          "tokens": [
            {
              "type": "word",
              "text": "TotalSales"
            },
            {
              "type": "word",
              "text": "where"
            },
            {
              "type": "word",
              "text": "city"
            },
            {
              "type": "word",
              "text": "="
            },
            {
              "type": "word",
              "text": "saint-petersburg"
            }
          ],
          "editedToken": -1
        }""".stripMargin) {
      assert(status == 200)
      println(body)
    }
  }

  private def postJson[A](url: String, json: String)(f : => A) : A = {
    post(url, headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json"), body = json)(f)
  }
}
