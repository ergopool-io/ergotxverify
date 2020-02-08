package controllers

import io.circe.Json
import javax.inject._
import org.ergoplatform.appkit.impl.Verification
import play.api._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  private val shit: String =
    """|{
       |  "id": "608d31bf581cf21cc3f5c5f33c90d72d67d992b5ef0019d032c511f1c4040a68",
       |  "inputs": [
       |    {
       |      "boxId": "0f18dae8ff9f99021ddc4073eb0cbea937a106a86547f90e51b67e5828f4b280",
       |      "spendingProof": {
       |        "proofBytes": "45d1d74415e0a1d1330b1d13b3bc6c7a26078deb11cff43a3a5a2cdfe24d2f3e9cf6be6b1dd14ddff56bc7d7252a49961001cb16b41a4a6e",
       |        "extension": {}
       |      }
       |    }
       |  ],
       |  "dataInputs": [],
       |  "outputs": [
       |    {
       |      "boxId": "f08c98851e67f96ceefdfa707c05c17e78d8c81c7555fbc1b03fee786c6a4e59",
       |      "value": 100000000,
       |      "ergoTree": "10020702e13083ad2747203c28d079ee5e5488f6e23ddd783c309c46a96ebb407b93038408cd02e13083ad2747203c28d079ee5e5488f6e23ddd783c309c46a96ebb407b930384ea02d193db6906db6503fe73007301",
       |      "creationHeight": 99328,
       |      "assets": [],
       |      "additionalRegisters": {},
       |      "transactionId": "608d31bf581cf21cc3f5c5f33c90d72d67d992b5ef0019d032c511f1c4040a68",
       |      "index": 0
       |    },
       |    {
       |      "boxId": "d7796b4042f9a2d039485e366a721438fcd7b962c657a42bc94172a5e1041e4a",
       |      "value": 1000000,
       |      "ergoTree": "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304",
       |      "creationHeight": 99328,
       |      "assets": [],
       |      "additionalRegisters": {},
       |      "transactionId": "608d31bf581cf21cc3f5c5f33c90d72d67d992b5ef0019d032c511f1c4040a68",
       |      "index": 1
       |    },
       |    {
       |      "boxId": "7d2cb5704e584fc32c7db4dbe75d6bfd5a2fc9766b88215a1d721dfe38a9d13e",
       |      "value": 899000000,
       |      "ergoTree": "0008cd02e13083ad2747203c28d079ee5e5488f6e23ddd783c309c46a96ebb407b930384",
       |      "creationHeight": 99328,
       |      "assets": [],
       |      "additionalRegisters": {},
       |      "transactionId": "608d31bf581cf21cc3f5c5f33c90d72d67d992b5ef0019d032c511f1c4040a68",
       |      "index": 2
       |    }
       |  ]
       |}
       |""".stripMargin


  /**
   * verifies the provided transaction using custom context with provided minerPk
   * body must have the following structure:
   * {
   *  "minerPk": "",
   *  "transaction": {
   *    the transaction
   *  }
   * }
   */
  def verify_tx() = Action { implicit request: Request[AnyContent] =>
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
