package no.nav.fo.miaindekserer.helpers

import java.util.*


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

operator fun Date.plus(ms: Long): Long = this.time + ms
operator fun Date.minus(ms: Long): Long = this.time - ms
operator fun Date.minus(date: Date): Long = this.time - date.time
operator fun Date.plus(date: Date): Long = this.time + date.time
operator fun Date.compareTo(now: Long): Int = this.time.compareTo(now)
operator fun Long.compareTo(date: Date): Int = this.compareTo(date.time)







