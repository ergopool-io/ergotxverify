package controllers

import io.circe.Json
import javax.inject._
import org.ergoplatform.appkit.impl.Verification
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class VerifyController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  /**
   * verifies the provided transaction using custom context with provided minerPk
   * body must have the following structure:
   * {
   * "minerPk": "",
   * "transaction": {
   * the transaction
   * }
   * }
   */
  def verify_tx: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val verifier = new Verification
    val js = io.circe.parser.parse(request.body.asJson.get.toString()).getOrElse(Json.Null)
    val minerPk = js.hcursor.downField("minerPk").as[String].getOrElse(null)
    val txs = js.hcursor.downField("transaction").as[Json].getOrElse(null)
    if (minerPk == null || txs == null) {
      BadRequest(
        s"""
           |{
           |  "success": false,
           |  "message": "minerPk and transaction must be present."
           |}
           |""".stripMargin
      ).as("application/json")

    } else {

      try {
        val result = verifier.verify(txs.noSpaces, minerPk)
        Ok(
          s"""
             |{
             |  "success": true,
             |  "verified": ${result}
             |}
             |""".stripMargin
        ).as("application/json")

      } catch {
        case ex =>
          ex.getStackTrace
          BadRequest(
            s"""
               |{
               |  "success": false,
               |  "message": "${ex.getMessage}"
               |}
               |""".stripMargin
          ).as("application/json")
      }
    }
  }

  def get_id: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val verifier = new Verification
    val js = io.circe.parser.parse(request.body.asJson.get.toString()).getOrElse(Json.Null)
    if (js == Json.Null) {
      BadRequest(
        s"""
           |{
           |  "success": false,
           |  "message": "transaction must be present."
           |}
           |""".stripMargin
      ).as("application/json")

    } else {
      try {
        val result = verifier.getTxId(js.noSpaces)
        Ok(
          s"""
             |{
             |  "success": true,
             |  "id": "${result}"
             |}
             |""".stripMargin
        ).as("application/json")

      } catch {
        case ex =>
          ex.getStackTrace
          BadRequest(
            s"""
               |{
               |  "success": false,
               |  "message": "${ex.getMessage}"
               |}
               |""".stripMargin
          ).as("application/json")
      }
    }

  }

  def address_to_pk: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val verifier = new Verification
    var js = request.body.asJson.get.toString()
    js = js.slice(1, js.length - 1)
    if (js == null) {
      BadRequest(
        s"""
           |{
           |  "success": false,
           |  "message": "address must be present!"
           |}
           |""".stripMargin
      ).as("application/json")

    } else {
      try {
        val result = verifier.addressToPk(js)
        Ok(
          s"""
             |{
             |  "success": true,
             |  "id": "${result}"
             |}
             |""".stripMargin
        ).as("application/json")

      } catch {
        case ex =>
          ex.getStackTrace
          BadRequest(
            s"""
               |{
               |  "success": false,
               |  "message": "${ex.getMessage}"
               |}
               |""".stripMargin
          ).as("application/json")
      }
    }

  }
}
