import com.google.gson.Gson
import no.nav.fo.miaindekserer.Statestikk
import no.nav.fo.miaindekserer.helpers.*
import no.nav.fo.miaindekserer.statestikkMapping
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.logging.log4j.LogManager
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import java.io.InputStream
import java.time.LocalDateTime

private val gson = Gson()
private val csvFormat = CSVFormat
    .DEFAULT
    .withFirstRecordAsHeader()
    .withIgnoreHeaderCase()
    .withTrim()

private val logger = LogManager.getLogger()

fun indekserStattestikk(stream: InputStream, esClient: RestHighLevelClient, alias: String): Boolean {
    val bufferedReader = stream.bufferedReader()

    val csvParser = CSVParser(bufferedReader, csvFormat)
    val index = alias + LocalDateTime.now().toString().replace(":", "_").toLowerCase()
    esClient.createIndice(
        name = index,
        jsonMapping = statestikkMapping
    )

    val failurs = csvParser
        .chunked(100) { csvList ->
            val requests = csvList
                .asSequence()
                .map(::getData)
                .map(gson::toJson)
                .map {
                    insertRequest(
                        index = index,
                        jsonObject = it
                    )
                }.toList()

            esClient.bulk(requests = requests)?.hasFailures() ?: true
        }

    esClient.replaceIndexForAlias(alias, index)

    val sucsess =  !failurs.contains(true)

    if(sucsess) {
        logger.info("indeksering vewlykket av $alias")
    }else {
        logger.error("indeksering feiler av $alias")
    }

    return sucsess
}

private fun RestHighLevelClient.replaceIndexForAlias(alias: String, nyIndex: String) {
    val removeOldIndexes = IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
        .alias(alias)
        .index("*")

    val addNewIndex = IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
        .alias(alias)
        .index(nyIndex)

    val request = IndicesAliasesRequest()
        .addAliasAction(removeOldIndexes)
        .addAliasAction(addNewIndex)

    this
        .indices()
        .updateAliases(request, RequestOptions.DEFAULT)

    this
        .indices()
        .delete(DeleteIndexRequest("${alias}*,-$nyIndex"), RequestOptions.DEFAULT)
}


private fun getData(it: CSVRecord): Statestikk {
    val styrk = it[3]!!

    return Statestikk(
        periode = it[0],
        fylkesnummer = it[1],
        komuneNumer = it[2],
        styrkKode = styrk,
        hovedkategori = getUnderkategori(styrk) ?: "ikke identifiserbare",
        underkattegori = getUnderkategori(styrk) ?: "ikke identifiserbare",
        antall = it[4].toInt()
    )
}

