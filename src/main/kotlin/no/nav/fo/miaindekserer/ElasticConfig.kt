package no.nav.fo.miaindekserer

import no.nav.fo.miaindekserer.helpers.getProp
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.xcontent.XContentType

val esUri = getProp("ES_HOST", "tpa-miasecsok-elasticsearch.tpa.svc.nais.local")

fun elasticClient(): RestHighLevelClient =
    RestHighLevelClient(
        RestClient
            .builder(
                HttpHost(
                    esUri,
                    getProp("ES_PORT", "9200").toInt(),
                    getProp("ES_SCHEAM", "http")
                )
            )
            .setHttpClientConfigCallback(HttpClientConfigCallback())
    )

class HttpClientConfigCallback : RestClientBuilder.HttpClientConfigCallback {
    override fun customizeHttpClient(httpAsyncClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder =
        httpAsyncClientBuilder.setDefaultCredentialsProvider(createCredentialsProvider())

    private fun createCredentialsProvider(): BasicCredentialsProvider {
        val credentialsProvider = BasicCredentialsProvider()
        val credentials = UsernamePasswordCredentials(
            getProp("ES_USER"),
            getProp("ES_PASSWORD")
        )
        credentialsProvider.setCredentials(AuthScope.ANY, credentials)
        return credentialsProvider
    }
}

fun RestHighLevelClient.oppretStillingerIndex() {
    val create = CreateIndexRequest(stillingsIndex)
    create.mapping(
        doc,
        stillingerMapping, XContentType.JSON
    )

    val result = this.indices().create(create, RequestOptions.DEFAULT)
    if (!result.isAcknowledged) throw RuntimeException("klarte ikke lage index")
}

fun RestHighLevelClient.finnesIndex(index: String) = this
    .indices()
    .exists(GetIndexRequest().indices(index), RequestOptions.DEFAULT)
