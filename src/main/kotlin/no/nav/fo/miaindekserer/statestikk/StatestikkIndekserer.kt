import com.google.gson.Gson
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import no.nav.fo.miaindekserer.Statestikk
import no.nav.fo.miaindekserer.config.sftp
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
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.time.LocalDateTime

private val gson = Gson()
private val csvFormat = CSVFormat
    .DEFAULT
    .withFirstRecordAsHeader()
    .withIgnoreHeaderCase()
    .withTrim()

private val logger = LogManager.getLogger()

fun indekser(esClient: RestHighLevelClient, session: Session) {
    return sftp(session) { sftp ->
        val filenames = sftp.ls(".")
            .mapNotNull {
                (it as ChannelSftp.LsEntry?)?.filename
            }

        val indeksAlias = filenames
            .map { esClient.indekserStattestikk(stream = sftp.get(it), alias = it) }

        esClient.
            replaceIndexForAlias(indeksAlias)

        filenames.map {
            sftp.rm(it)
        }

    }
}

fun RestHighLevelClient.indekserStattestikk(stream: InputStream, alias: String): IndexAlias {
    val bufferedReader = stream.bufferedReader()

    val csvParser = CSVParser(bufferedReader, csvFormat)
    val index = alias + LocalDateTime.now().toString().replace(":", "_").toLowerCase()
    this.createIndice(
        name = index,
        jsonMapping = statestikkMapping
    )

    val antall = csvParser
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


            val response = this.bulk(requests = requests)
            if(response?.hasFailures() == true) {
                logger.error("indeksering fra datavarehus feilet")
                logger.error(response.buildFailureMessage())

                throw IllegalStateException("indeksereing fra datavarehus feilet")
            }
            requests.size
        }.sum()

    logger.info("indeksering av alias: $alias som indeks $index fra datavarehus velykket")
    logger.info("indeks: $index indeksert med $antall inslag")

    return IndexAlias(
        index= index,
        alias = alias
    )
}

fun RestHighLevelClient.replaceIndexForAlias(indeksAliastList: List<IndexAlias>) {
    val request = indeksAliastList.fold(IndicesAliasesRequest()) { req, indexAlias ->

        req.addAliasAction(
            IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                .alias(indexAlias.alias)
                .index("*")
        )

        req.addAliasAction(
            IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                .alias(indexAlias.alias)
                .index(indexAlias.index)
        )

    }

    val updateAliases = this
        .indices()
        .updateAliases(request, RequestOptions.DEFAULT)

    if(!updateAliases.isAcknowledged) {
        logger.error("noe feil med oppdatering av alias")
        logger.warn(updateAliases.toString())
        throw RuntimeException("feil ved endring av alias")
    }

    indeksAliastList.forEach{
        val response = this
            .indices()
            .delete(DeleteIndexRequest("${it.alias}*,-${it.index}"), RequestOptions.DEFAULT)

        if(!response.isAcknowledged){
            logger.error("noe feil med sletting av gamle indekser")
            logger.warn(updateAliases.toString())
            throw RuntimeException("feil ved sletting av gamle indekser")
        }
    }


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




data class IndexAlias(val index: String, val alias: String)
