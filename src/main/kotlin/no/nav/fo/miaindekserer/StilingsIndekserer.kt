package no.nav.fo.miaindekserer

import com.google.gson.Gson
import no.nav.fo.miaindekserer.helpers.midenattIGard
import org.apache.logging.log4j.LogManager
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.max.Max
import org.elasticsearch.search.builder.SearchSourceBuilder


val startPam = "2018-01-01T10:00"

private val gson = Gson()

private val antall = 1000

private val logger = LogManager.getLogger("pamIndekser")!!


fun indekserStillingerFraPam(esClient: RestHighLevelClient) {
    slettGamleStillinger(esClient)

    var side = 0
    var updatedSince = hentNyesteOppdatert(esClient)

    do {
        val stillinger = hentStillingerFraPam(
            side = side,
            updatedSince = updatedSince,
            perSide = antall
        )

        val response = esClient
            .bulk(bulkUpsertRequest(stillinger), RequestOptions.DEFAULT)

        if (response.hasFailures()) {
            logger.warn("""indeksering har feil ${response.buildFailureMessage()}""")
        }
        logger.info("indekserte: ${response.items.filter { !it.isFailed }.size} velykket")


        if (stillinger.first().gyldigTil == stillinger.last().gyldigTil) {
            side++
        } else {
            updatedSince = stillinger.last().gyldigTil
            side = 0
        }

    } while (stillinger.size == antall)
}

private fun bulkUpsertRequest(stillinger: List<Stilling>) = BulkRequest()
    .add(
        stillinger.map { stilling ->
            UpdateRequest(stillingsIndex, doc, stilling.id)
                .doc(gson.toJson(stilling), XContentType.JSON)
                .docAsUpsert(true)
        })!!

fun slettGamleStillinger(esClient: RestHighLevelClient) {
    val response = esClient.deleteByQuery(
        DeleteByQueryRequest(stillingsIndex)
            .setQuery(
                QueryBuilders.boolQuery()
                    .should(
                        QueryBuilders.boolQuery()
                            .must(QueryBuilders.rangeQuery("gyldigTil").lte(midenattIGard()))
                            .must(QueryBuilders.rangeQuery(oppdatert).lt(hentNyesteOppdatert(esClient)))
                    )
                    .should(
                        QueryBuilders.boolQuery()
                            .must(QueryBuilders.matchQuery("active", false))
                            .must(QueryBuilders.rangeQuery(oppdatert).lt(hentNyesteOppdatert(esClient)))
                    )
                    .minimumShouldMatch(1)
            ),
        RequestOptions.DEFAULT
    )

    logger.info("""deletetet : ${response.deleted} stillinger""")
}

fun hentNyesteOppdatert(esClient: RestHighLevelClient): String {

    val sistOppdatert = esClient
        .search(
            SearchRequest()
                .indices(stillingsIndex)
                .source(
                    SearchSourceBuilder()
                        .aggregation(AggregationBuilders.max(oppdatert).field(oppdatert))
                        .size(0)
                ),
            RequestOptions.DEFAULT
        )!!
        .aggregations
        .get<Max>(oppdatert)
        .valueAsString
        .removeSuffix("Z")

    return if (sistOppdatert == "-Infinity") {
        startPam
    } else {
        sistOppdatert
    }
}
