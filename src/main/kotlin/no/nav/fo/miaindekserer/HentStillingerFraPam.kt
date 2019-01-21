package no.nav.fo.miaindekserer


import com.google.gson.Gson
import no.nav.fo.miaindekserer.helpers.ingenKomune
import no.nav.fo.miaindekserer.helpers.komuneNrTilFylkesNr
import no.nav.fo.miaindekserer.helpers.styrkTilHovedkategori
import no.nav.fo.miaindekserer.helpers.styrkTilUnderkategori


private val punctRegex = """\.""".toRegex()
private val gson = Gson()

data class Root(val content: MutableList<JsonStilling>)
data class JsonStilling(
    val uuid: String,
    val status: String,
    val privacy: String,
    val expires: String,
    val updated: String,
    val categoryList: MutableList<Category?>?,
    val properties: Properties?,
    val location: Location?)
data class Category(val code: String?)
data class Properties(val positioncount: String?)
data class Location(val municipalCode: String?)

fun hentStillingerFraPam(side: Int, updatedSince: String, perSide: Int): List<Stilling> {
    val response = khttp.get(
        url = "$pamUrl/api/v1/ads",
        params = mapOf(
            "updatedSince" to updatedSince,
            "sort" to "updated,asc",
            "page" to side.toString(),
            "size" to perSide.toString()
        )
    )

    return gson
        .fromJson(response.text, Root::class.java)!!
        .content
        .map {
            val styrk = it.categoryList
                ?.mapNotNull { it?.code }
                ?.mapNotNull { s -> s.split(punctRegex).first() } ?: emptyList()

            val komuineNr = it.location?.municipalCode?: ingenKomune

            Stilling(
                    id = it.uuid,
                    active = it.status == "AKTIVE",
                    public = it.privacy == "SHOW_ALL",
                    antall = it.properties?.positioncount?.toIntOrNull()?: 1,
                    styrk = styrk,
                    hovedkategori = styrk.mapNotNull { styrkKode -> styrkTilHovedkategori[styrkKode] }.toList(),
                    underkattegori = styrk.mapNotNull { styrkKode -> styrkTilUnderkategori[styrkKode] }.toList(),
                    komuneNumer = komuineNr,
                    fylkesnr = komuneNrTilFylkesNr[komuineNr],
                    gyldigTil = it.expires,
                    oppdatert = it.updated
                )
        }.filter { it.public }
}

