package testservers

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler

class TestNode() extends TestJettyServer {
  override val serverPort: Int = 9001
  override val serverName: String = "Test Node"

  override protected val server: Server = createServer()

  override protected val handler: ServletHandler = new ServletHandler()

  server.setHandler(handler)

  handler.addServletWithMapping(classOf[NodeServlets.InfoServlet], "/info")
  handler.addServletWithMapping(classOf[NodeServlets.LastHeadersServlet], "/blocks/lastHeaders/10")
  handler.addServletWithMapping(classOf[NodeServlets.BoxByIdOrdinaryServlet], "/utxo/byId/cd49e6bf930739e99e6b096979e4121102a50dd6b99eaa9e3b72e6d8dbd4a882")
  handler.addServletWithMapping(classOf[NodeServlets.BoxByIdContextServlet], "/utxo/byId/b5daf1407c3b158a9874ac8be2313a6b38a419853362eb34a6ce528744cf2352")

}

object NodeServlets {
  class InfoServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      val content = Files.readString(Paths.get("test/resources/info.json"), StandardCharsets.UTF_8)
      resp.getWriter.print(content)
    }
  }

  class LastHeadersServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      val content = Files.readString(Paths.get("test/resources/lastHeaders.json"), StandardCharsets.UTF_8)
      resp.getWriter.print(content)
    }
  }

  class BoxByIdOrdinaryServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      val content = Files.readString(Paths.get("test/resources/ordinaryBox.json"), StandardCharsets.UTF_8)
      resp.getWriter.print(content)
    }
  }

    class BoxByIdContextServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      val content = Files.readString(Paths.get("test/resources/contextBox.json"), StandardCharsets.UTF_8)
      resp.getWriter.print(content)
    }
  }
}


