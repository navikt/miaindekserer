package no.nav.fo.miaindekserer.helpers

import java.time.LocalDate
import java.time.LocalDateTime

fun getOptionalProperty(propName: String): String? =
    System.getenv(propName) ?: System.getProperty(propName)

fun getProp(propName: String, default: String) = getOptionalProperty(propName) ?: default

fun getProp(propName: String) =
    getOptionalProperty(propName) ?: throw IllegalArgumentException("Missing required property: $propName")


fun midenattIGard(): LocalDateTime {
    return LocalDate.now()
        .atStartOfDay()
        .minusDays(1)
}
