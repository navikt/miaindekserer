package no.nav.fo.miaindekserer

val stillingsIndex = "stillinger4"
val oppdatert = "oppdatert"
val doc = "_doc"

data class Stilling(
    val id: String,
    val active: Boolean,
    val public: Boolean,
    val antall: Int,
    val styrkKode: List<String>,
    val hovedkategori: List<String>,
    val underkattegori: List<String>,
    val komuneNumer: String?,
    val fylkesnr: String?,
    val gyldigTil: String,
    val oppdatert: String
)

val stillingerMapping =
    """
        {
          "_doc": {
            "properties": {
              "id": { "type": "text" },
              "active": { "type": "boolean" },
              "public": { "type": "boolean" },
              "antall": { "type": "short" },
              "styrkKode": { "type": "keyword" },
              "hovedkategori": { "type": "keyword" },
              "underkattegori": { "type": "keyword" },
              "komuneNumer": { "type": "keyword" },
              "fylkesnr": { "type": "keyword" },
              "gyldigTil": { "type": "date" },
              "oppdatert": { "type": "date" }
            }
          }
        }""".trimIndent()

data class Statestikk(
    val periode: String,
    val fylkesnummer: String?,
    val komuneNumer: String?,
    val antall: Int,
    val hovedkategori: String,
    val underkattegori: String,
    val styrkKode: String
)

val statestikkMapping =
    """
        {
          "_doc": {
            "properties": {
              "periode"       : { "type": "keyword" },
              "fylkesnummer"  : { "type": "keyword" },
              "komuneNumer"   : { "type": "keyword" },
              "antall"        : { "type": "integer" },
              "hovedkategori" : { "type": "keyword" },
              "underkattegori": { "type": "keyword" },
              "styrkKode"     : { "type": "keyword" }
            }
          }
        }
    """.trimIndent()
