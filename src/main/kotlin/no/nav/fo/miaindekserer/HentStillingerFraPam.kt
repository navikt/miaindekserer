package no.nav.fo.miaindekserer


import no.nav.fo.miaindekserer.helpers.getProp
import no.nav.fo.miaindekserer.helpers.komuneNrTilFylkesNr
import no.nav.fo.miaindekserer.helpers.styrkTilHovedkategori
import no.nav.fo.miaindekserer.helpers.styrkTilUnderkategori
import org.json.JSONObject


private val punctRegex = """\.""".toRegex()



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

    val json = response.jsonObject["content"] as Iterable<JSONObject>


    return json.map {
        val porps = it["properties"] as JSONObject
        val antall = if (porps.has("positioncount")) {
            porps.getInt("positioncount")
        } else {
            1
        }

        val styrk = (it["categoryList"] as Iterable<JSONObject>)
            .map { categoryList -> categoryList["code"] as String }
            .map { s -> s.split(punctRegex).first() }.toList()

        val komuneNumer = (it["location"] as JSONObject)["municipalCode"].toString()
        Stilling(
            id = it["uuid"] as String,
            active = it["status"] == "AKTIVE",
            public = it["privacy"] == "SHOW_ALL",
            antall = antall,
            styrk = styrk,
            hovedkategori = styrk.mapNotNull { styrkKode -> styrkTilHovedkategori[styrkKode] }.toList(),
            underkattegori = styrk.mapNotNull { styrkKode -> styrkTilUnderkategori[styrkKode] }.toList(),
            komuneNumer = komuneNumer,
            fylkesnr = komuneNrTilFylkesNr[komuneNumer],
            gyldigTil = it["expires"] as String,
            oppdatert = it["updated"] as String
        )
    }.filter { it.public }
}

