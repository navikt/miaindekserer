package no.nav.fo.miaindekserer

import no.nav.fo.miaindekserer.helpers.getProp
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient



fun elasticClient(): RestHighLevelClient =
        RestHighLevelClient(
            RestClient
                .builder(HttpHost(getProp("esHost", "tpa-miasecsok-elasticsearch.tpa.svc.nais.local"),
                    getProp("esPort", "9200").toInt(),
                    getProp("esScheam", "http")))
                .setHttpClientConfigCallback(HttpClientConfigCallback())
        )


class HttpClientConfigCallback : RestClientBuilder.HttpClientConfigCallback {
    override fun customizeHttpClient(httpAsyncClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder =
        httpAsyncClientBuilder.setDefaultCredentialsProvider(createCredentialsProvider())

    private fun createCredentialsProvider(): BasicCredentialsProvider {
        val credentialsProvider = BasicCredentialsProvider()
        val credentials = UsernamePasswordCredentials(
            getProp("esUser"),
            getProp("esPassword")
        )
        credentialsProvider.setCredentials(AuthScope.ANY, credentials)
        return credentialsProvider
    }
}

