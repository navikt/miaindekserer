package no.nav.fo.miaindekserer

import no.nav.fo.miaindekserer.helpers.addShutdownHook
import no.nav.fo.miaindekserer.helpers.getProp
import no.nav.fo.miaindekserer.helpers.jetty
import no.nav.fo.miaindekserer.helpers.kjort
import org.apache.logging.log4j.LogManager
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType


import kotlin.concurrent.fixedRateTimer
import java.util.*


val stillingsIndex = "stillinger"
val oppdatert = "oppdatert"
val doc = "_doc"


val pamUrl = getProp("PAM_URL", "http://pam-ad.default.svc.nais.local")
val esUri = getProp("ES_HOST", "tpa-miasecsok-elasticsearch.tpa.svc.nais.local")

private val logger = LogManager.getLogger("main")!!

fun main(args: Array<String>) {
    logger.info("Starter")
    logger.info("esUrl = $esUri")
    logger.info("pamURL = $pamUrl")

    addShutdownHook()

    val esClient = elasticClient()

    if (!esClient.finnesIndex(stillingsIndex)) {
        esClient.oppretStillingerIndex()
    }

    val sistOppdatertPam = kjort(Date(0), 50000)
    fixedRateTimer(
        name = "pamStillingOppdaterer",
        initialDelay = 100,
        period = 50000
    )
    {
        try {
            logger.info("starter indeksering")
            indekserStillingerFraPam(esClient)
            logger.info("indeksering ferdig")
        } catch (e: Exception) {
            logger.error("indeksering feilet", e)
            System.exit(1)
        }
        sistOppdatertPam.sistKjort = Date()
    }

    jetty(sistOppdatertPam, esClient)
}
