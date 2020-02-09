package controllers

import java.io.File

import akka.util.ByteString
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.{BeforeAndAfterAll, PrivateMethodTester}
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.json.Json
import java.io.FileInputStream

import play.api.mvc.RawBuffer
import testservers.TestNode

/**
 * Check if proxy server would pass any POST or GET requests with their header and body with any route to that route of the specified node
 */
class VerifyControllerSpec extends PlaySpec with BeforeAndAfterAll with PrivateMethodTester {

    val node: TestNode = new TestNode()
    node.startServer()

  val controller: VerifyController = new VerifyController(stubControllerComponents())

  override def afterAll(): Unit = {
    node.stopServer()
    super.afterAll()
  }

  "verify_tx should" should {

    /**
     * Purpose: Verify a valid transaction with ordinary input box.
     * Prerequisites: node server must be up and running.
     * Scenario: a valid transaction with ordinary input boxes must pass verification.
     * Test Conditions:
     * * true must be returned
     */
    "verify a valid ordinary transaction" in {
      val stream = new FileInputStream("test/resources/ordinaryTransaction.json")
      val json = try {  Json.parse(stream) } finally { stream.close() }
      val fakeRequest = FakeRequest(POST, "/verify").withJsonBody(json)
      val response = controller.verify_tx().apply(fakeRequest)
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      val js = io.circe.parser.parse(contentAsString(response)).getOrElse(null)
      js.noSpaces must be ("{\"success\":true,\"verified\":true}")
    }

    "verify a valid context transaction with valid pk" in {
      val stream = new FileInputStream("test/resources/contextTransaction.json")
      val json = try {  Json.parse(stream) } finally { stream.close() }
      val fakeRequest = FakeRequest(POST, "/verify").withJsonBody(json)
      val response = controller.verify_tx().apply(fakeRequest)
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      val js = io.circe.parser.parse(contentAsString(response)).getOrElse(null)
      js.noSpaces must be ("{\"success\":true,\"verified\":true}")
    }

    "verify a valid context transaction with invalid pk" in {
      val stream = new FileInputStream("test/resources/invalidContextTransaction.json")
      val json = try {  Json.parse(stream) } finally { stream.close() }
      val fakeRequest = FakeRequest(POST, "/verify").withJsonBody(json)
      val response = controller.verify_tx().apply(fakeRequest)
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      val js = io.circe.parser.parse(contentAsString(response)).getOrElse(null)
      js.noSpaces must be ("{\"success\":true,\"verified\":false}")
    }

    "fail because transaction not presented" in {
      val stream = new FileInputStream("test/resources/malformedRequest.json")
      val json = try {  Json.parse(stream) } finally { stream.close() }
      val fakeRequest = FakeRequest(POST, "/verify").withJsonBody(json)
      val response = controller.verify_tx().apply(fakeRequest)
      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("application/json")
      val js = io.circe.parser.parse(contentAsString(response)).getOrElse(null)
      js.noSpaces must include ("\"success\":false")
    }

    "fail because minerPk not presented" in {
      val stream = new FileInputStream("test/resources/malformedRequestNoPk.json")
      val json = try {  Json.parse(stream) } finally { stream.close() }
      val fakeRequest = FakeRequest(POST, "/verify").withJsonBody(json)
      val response = controller.verify_tx().apply(fakeRequest)
      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("application/json")
      val js = io.circe.parser.parse(contentAsString(response)).getOrElse(null)
      js.noSpaces must include ("\"success\":false")
    }

    "fail because transaction is malformed" in {
      val stream = new FileInputStream("test/resources/contextTransactionMalformed.json")
      val json = try {  Json.parse(stream) } finally { stream.close() }
      val fakeRequest = FakeRequest(POST, "/verify").withJsonBody(json)
      val response = controller.verify_tx().apply(fakeRequest)
      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("application/json")
      val js = io.circe.parser.parse(contentAsString(response)).getOrElse(null)
      js.noSpaces must include ("\"success\":false")
    }
  }
}
