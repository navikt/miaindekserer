package no.nav.fo.miaindekserer.helpers

import no.nav.fo.miaindekserer.doc
import org.apache.logging.log4j.LogManager
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType

private val logger = LogManager.getLogger()

fun RestHighLevelClient.createIndice(name: String, jsonMapping: String, mappingFor: String = doc): CreateIndexResponse {
    val response = this.indices().create(
        CreateIndexRequest(name)
            .mapping(mappingFor, jsonMapping, XContentType.JSON),
        RequestOptions.DEFAULT
    )

    if (response.isAcknowledged) {
        logger.info("$name vellyket opprettet")
    } else {
        logger.warn("$name ikke Acknowledged")
    }

    return response
}

fun RestHighLevelClient.bulk(requests: List<DocWriteRequest<*>>): BulkResponse? {
    if (requests.isEmpty()) {
        return null
    }

    val bulk = this.bulk(BulkRequest().add(requests), RequestOptions.DEFAULT)

    if (bulk.hasFailures()) {
        logger.warn("feil ved bulk request")
        logger.warn(bulk.buildFailureMessage())
        logger.info("""sukses for: ${bulk.items.filter { !it.isFailed }.size} requests""")
    } else {
        logger.debug("velyket prosesert ${bulk.items.size} requests")
    }

    return bulk
}

fun insertRequest(index: String, jsonObject: String, type: String = doc) =
    IndexRequest(index, type)
        .source(jsonObject, XContentType.JSON)!!
