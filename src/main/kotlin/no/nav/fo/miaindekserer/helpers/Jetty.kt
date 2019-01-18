package no.nav.fo.miaindekserer.helpers

import io.prometheus.client.exporter.MetricsServlet
import io.prometheus.client.hotspot.DefaultExports
import no.nav.fo.miaindekserer.hentNyesteOppdatert
import no.nav.fo.miaindekserer.hentStillingerFraPam
import no.nav.fo.miaindekserer.startPam
import org.apache.logging.log4j.LogManager
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.elasticsearch.client.RestHighLevelClient
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun jetty(
    sistOppdatertPam: kjort,
    esClient: RestHighLevelClient
) {
    val server = Server(1234)

    val context = ServletContextHandler()
    context.contextPath = "/"
    server.handler = context

    context.addServlet(ServletHolder(IsAlive(sistOppdatertPam)), "/isAlive")
    context.addServlet(ServletHolder(IsRedy(esClient)), "/isRedy")
    context.addServlet(ServletHolder(MetricsServlet()), "/metrics")
    DefaultExports.initialize()

    server.start()
    server.join()
}

data class kjort(var sistKjort: Date, val period: Long) {
    fun healty(multiplier: Double = 3.0) =
        Date().time - period * multiplier < sistKjort.time
}

class IsRedy(esClient: RestHighLevelClient) : HttpServlet() {
    val client = esClient

    var esReady = false
    var pamRedy = false

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {

        if (!esReady) {
            hentNyesteOppdatert(esClient = client)
            esReady = true
        }

        if (!pamRedy) {
            hentStillingerFraPam(side = 0, updatedSince = startPam, perSide = 10)
            pamRedy = true
        }

        if(pamRedy && esReady) {
            resp.writer.println("Is Redy!")
        }else {
            resp.status = 500
            resp.writer.println("pamredy er: $pamRedy")
            resp.writer.println("esredy er: $esReady")
        }
    }
}

class IsAlive(val sistIndeksertFraPam: kjort) : HttpServlet() {
    val logger = LogManager.getLogger(this.javaClass.name)

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val pamHealty = sistIndeksertFraPam.healty(50.0)

        if(pamHealty) {
            resp.writer.println("Healty")
            resp.writer.println("pam sist kjort ${sistIndeksertFraPam.sistKjort}")
        } else {
            logger.error("for lenge siden indekseringsjobben har skjørt, sist kjort ${sistIndeksertFraPam.sistKjort}")

            resp.status = 500
            resp.writer.println("IKKE FRISK")
            resp.writer.println("pam sist kjort ${sistIndeksertFraPam.sistKjort}")
        }
    }
}
