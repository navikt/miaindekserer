package no.nav.fo.miaindekserer

import no.nav.fo.miaindekserer.helpers.*
import org.apache.logging.log4j.LogManager

import kotlin.concurrent.fixedRateTimer
import java.util.*

private val logger = LogManager.getLogger()

fun main(args: Array<String>) {
    logger.info("Starter")
    logger.info("esUrl = $esUri")
    logger.info("pamURL = $pamUrl")

    addShutdownHook()

    val esClient = elasticClient()


    if (!esClient.finnesIndex(stillingsIndex)) {
        esClient.createIndice(stillingsIndex, stillingerMapping)
    }

    val sistOppdatertPam = kjort(50000, 10, "pam indekserer")
    fixedRateTimer(
        name = "pamStillingOppdaterer",
        initialDelay = 100,
        period = 50000
    )
    {
        try {
            logger.info("starter indeksering")
            indekserStillingerFraPam(esClient, sistOppdatertPam)
            logger.info("indeksering ferdig")
        } catch (e: Exception) {
            logger.error("indeksering feilet", e)
            System.exit(1)
        }
    }

    jetty(esClient = esClient, sistOppdatertPam = sistOppdatertPam)
}
