package org.ergoplatform.appkit.impl

import java.io.StringReader

import com.typesafe.config.ConfigFactory
import org.ergoplatform.appkit._
import org.ergoplatform.appkit.config.ErgoToolConfig
import org.ergoplatform.restapi.client.{ApiClient, Parameters}
import org.ergoplatform.validation.ValidationRules
import org.ergoplatform.wallet.interpreter.ErgoInterpreter
import org.ergoplatform.wallet.protocol.context.{ErgoLikeParameters, TransactionContext}
import org.ergoplatform.{ErgoBox, ErgoLikeContext, ErgoLikeTransaction}
import play.api.Configuration
import scorex.crypto.authds.ADDigest
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import sigmastate.eval.CPreHeader
import sigmastate.interpreter.{ContextExtension, ProverResult}

class Verification {

  /**
   * verifies a transaction in custom context; throws errors in case of invalid inputs
   * if someday appkit interface get changed for any reason, this must be changed too!
   * @param txs tansaction to be verified
   * @param minerPk miner public key to put in the custom context
   * @return true if verification succeed, false otherwise
   */
  def verify(txs: String, minerPk: String): Boolean = {
    val conf = getNodeConf()

    val ergoClient = RestApiErgoClient.create(conf.getNode)
    ergoClient.execute((ctx: BlockchainContext) => {
      val ctxImpl = ctx.asInstanceOf[BlockchainContextImpl]

      val transaction = ctx.signedTxFromJson(txs).asInstanceOf[SignedTransactionImpl].getTx
      val ergoTransaction = ScalaBridge.isoErgoTransaction.from(transaction)

      val ergoParameters = getErgoLikeParameters(ctx)
      val verifier: ErgoInterpreter = ErgoInterpreter(ergoParameters)
      verifier.IR.resetContext() // ensure there is no garbage in the IRContext


      val client = new ApiClient(conf.getNode.getNodeApi.getApiUrl)
      val retrofit = client.getAdapterBuilder.client(client.getOkBuilder.build).build
      var boxesToSpend = IndexedSeq[ErgoBox]()
      ergoTransaction.getInputs.forEach(input => {
        val boxData = ErgoNodeFacade.getBoxById(retrofit, input.getBoxId)
        val ergoBox = ScalaBridge.isoErgoTransactionOutput.to(boxData)
        boxesToSpend = boxesToSpend :+ ergoBox
      })

      var dataBoxes = IndexedSeq[ErgoBox]()
      ergoTransaction.getDataInputs.forEach(input => {
        val boxData = ErgoNodeFacade.getBoxById(ctxImpl.getRetrofit, input.getBoxId)
        val ergoBox = ScalaBridge.isoErgoTransactionOutput.to(boxData)
        dataBoxes = dataBoxes :+ ergoBox
      })

      var res: Boolean = true
      for ((box, idx) <- boxesToSpend.zipWithIndex) {
        val input = ergoTransaction.getInputs.get(idx)
        val prov: ProverResult = ScalaBridge.isoSpendingProof.to(input.getSpendingProof)
        val context = getErgoLikeContext(ctxImpl, minerPk, boxesToSpend, dataBoxes, transaction, idx.toShort)
        val result = verifier.verify(box.ergoTree, context, prov, transaction.messageToSign)
        res = res & result.get._1
      }

      return res
    })

    false
  }

  def getTxId(txs: String): String = {
    val conf = getNodeConf()
    val ergoClient = RestApiErgoClient.create(conf.getNode)
    ergoClient.execute((ctx: BlockchainContext) => {
      val transaction = ctx.signedTxFromJson(txs).asInstanceOf[SignedTransactionImpl].getTx
      return Base16.encode(Blake2b256.hash(transaction.messageToSign))
    })
  }

  def getNodeConf(): ErgoToolConfig = {
    val config = Configuration(ConfigFactory.load()) // app configuration

    val node_host = config.getOptional[String]("node.host").orNull
    val node_port = config.getOptional[String]("node.port").orNull
    val secret = config.getOptional[String]("node.secret").orNull
    val network_type = config.getOptional[String]("node.network_type").orNull

    val node_conf =s"""{
                      |  "node": {
                      |    "nodeApi": {
                      |      "apiUrl": "$node_host:$node_port/",
                      |      "apiKey": "$secret"
                      |    },
                      |    "networkType": "$network_type"
                      |  }
                      |}""".stripMargin

    ErgoToolConfig.load(new StringReader(node_conf))
  }


  /**
   * creates ErgoLikeContext used in verifier
   * if someday appkit changes this for any reason, this must be changed too!
   * @param ctx current blockchain context
   * @param minerPk miner public key
   * @param boxesToSpend spent boxes in transaction
   * @param dataBoxes data boxes in transaction
   * @param trans the transaction itself
   * @param idx index of the box in `boxesToSpend` that contains the script we're evaluating
   * @return ErgoLikeContext object
   */
  def getErgoLikeContext(ctx: BlockchainContextImpl, minerPk: String, boxesToSpend: IndexedSeq[ErgoBox],
                         dataBoxes: IndexedSeq[ErgoBox], trans: ErgoLikeTransaction, idx: Short): ErgoLikeContext = {
    val par = getErgoLikeParameters(ctx)
    val inputsCost = boxesToSpend.size * par.inputCost
    val dataInputsCost = dataBoxes.size * par.dataInputCost
    val outputsCost = trans.outputCandidates.size * par.outputCost
    val initialCost: Long = inputsCost + dataInputsCost + outputsCost
    val pk = Address.create(minerPk).getPublicKeyGE
    val allHeaders = Iso.JListToColl(ScalaBridge.isoBlockHeader, ErgoType.headerType.getRType).to(ctx.getHeaders)
    val headers = allHeaders.slice(1, allHeaders.length)
    val h = ScalaBridge.isoBlockHeader.to(ctx.getHeaders.get(0))
    val pre = CPreHeader(h.version, h.parentId, h.timestamp, h.nBits, h.height, pk, h.votes)
    val transactionContext = TransactionContext(boxesToSpend, dataBoxes, trans, idx)
    new ErgoLikeContext(ErgoInterpreter.avlTreeFromDigest(ADDigest @@ JavaHelpers.getStateDigest(headers.apply(0).stateRoot)),
      headers,
      pre,
      transactionContext.dataBoxes,
      transactionContext.boxesToSpend,
      transactionContext.spendingTransaction,
      transactionContext.selfIndex,
      ContextExtension.empty,
      ValidationRules.currentSettings,
      par.maxBlockCost,
      initialCost
    )
  }

  /**
   * extracts necessary info to build ErgoLikeParameter instance
   * this code is based on how appkit does this
   * if someday appkit changes this for any reason, this must be changed too!
   * @param ctx BlockchainContext instance
   * @return ErgoLikeParameter instance
   */
  def getErgoLikeParameters(ctx: BlockchainContext): ErgoLikeParameters = {
    var ctxImpl = ctx.asInstanceOf[BlockchainContextImpl]

    new ErgoLikeParameters {
      val params: Parameters = ctxImpl.getNodeInfo.getParameters

      override def storageFeeFactor: Int = params.getStorageFeeFactor
      override def minValuePerByte: Int = params.getMinValuePerByte
      override def maxBlockSize: Int = params.getMaxBlockSize
      override def tokenAccessCost: Int = params.getTokenAccessCost
      override def inputCost: Int = params.getInputCost
      override def dataInputCost: Int = params.getDataInputCost
      override def outputCost: Int = params.getOutputCost
      override def maxBlockCost: Long = params.getMaxBlockCost.longValue()
      override def softForkStartingHeight: Option[Int] = Option.apply(0)
      override def softForkVotesCollected: Option[Int] = Option.apply(0)
      override def blockVersion: Byte = params.getBlockVersion.byteValue()
    }
  }
}
