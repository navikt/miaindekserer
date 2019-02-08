package no.nav.fo.miaindekserer.helpers

import indekserStattestikk
import io.prometheus.client.exporter.MetricsServlet
import io.prometheus.client.hotspot.DefaultExports
import no.nav.fo.miaindekserer.hentNyesteOppdatert
import no.nav.fo.miaindekserer.hentStillingerFraPamMedPrivate
import no.nav.fo.miaindekserer.startPam
import org.apache.logging.log4j.LogManager
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.elasticsearch.client.RestHighLevelClient
import java.util.*
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun jetty(
    sistOppdatertPam: kjort,
    esClient: RestHighLevelClient
) {
    val server = Server(8080)

    val context = ServletContextHandler()
    context.contextPath = "/"
    server.handler = context

    val a = ServletHolder(UploadeService(esClient))

    a.registration.setMultipartConfig(MultipartConfigElement("", 52428800, 52428800, 52428800))

    context.addServlet(ServletHolder(IsAlive(sistOppdatertPam)), "/isAlive")
    context.addServlet(ServletHolder(IsRedy(esClient)), "/isReady")
    context.addServlet(ServletHolder(MetricsServlet()), "/metrics")
    context.addServlet(a, "/uploade")
    DefaultExports.initialize()

    server.start()
    server.join()
}

class UploadeService(val esClient: RestHighLevelClient) : HttpServlet() {
    val logger = LogManager.getLogger()

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val alias = req.queryString.split("=")[1]
        logger.info("uploding file to alias $alias")
        val part = req.getPart("file")
        indekserStattestikk(stream = part.inputStream, esClient = esClient, alias = alias)

        resp.status = 200
        resp.writer.println("$alias suksefult oppdatert")
    }

}

data class kjort(val period: Long, val initialDilay: Long, val name: String) {
    private val created = Date()
    private var sistKjort: Date? = null

    fun kjort() {
        sistKjort = Date()
    }

    fun healty(multiplier: Double = 3.0): Boolean {
        return if(sistKjort == null) {
            created.time  > Date().time - (period * multiplier) - initialDilay - 1000
        } else {
            (sistKjort as Date).time > Date().time - (period * multiplier) - 1000
        }
    }

    fun status(): String {
        return if(sistKjort == null) {
            "$name ikke kjørt enda, created: $created"
        } else {
            "$name sist skjort: $sistKjort created: $created"
        }
    }
}

class IsRedy(esClient: RestHighLevelClient) : HttpServlet() {
    val logger = LogManager.getLogger()
    val client = esClient

    var esReady = false
    var pamRedy = false

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {

        if (!esReady) {
            hentNyesteOppdatert(esClient = client)
            esReady = true
        }

        if (!pamRedy) {
            hentStillingerFraPamMedPrivate(side = 0, updatedSince = startPam, perSide = 10)
            pamRedy = true
        }

        if (pamRedy && esReady) {
            resp.status = 200
            resp.writer.println("Is Redy!")
            logger.info("Is Ready")
        } else {
            logger.warn("ikke ready pam: $pamRedy, es: $esReady")
            resp.status = 500
            resp.writer.println("pamredy er: $pamRedy")
            resp.writer.println("esredy er: $esReady")
        }
    }
}

class IsAlive(val sistIndeksertFraPam: kjort) : HttpServlet() {
    val logger = LogManager.getLogger()

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {

        if(sistIndeksertFraPam.healty()) {
            logger.info("healty :) " + sistIndeksertFraPam.status())

            resp.status = 200
            resp.writer.println("Healty")
            resp.writer.println(sistIndeksertFraPam.status())
        } else {
            logger.warn("not healty " + sistIndeksertFraPam.status())

            resp.status = 500
            resp.writer.println("Not healty")
            resp.writer.println(sistIndeksertFraPam.status())
        }
    }
}
