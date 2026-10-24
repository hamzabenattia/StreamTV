package com.streamtv.app.data

import java.util.Locale

object Countries {
    /** Shown first in the country filter: Tunisia, the Maghreb, the rest of the Arab world, then big diaspora countries. */
    val PRIORITY = listOf(
        "tn", "dz", "ma", "ly", "mr", "eg", "sa", "ae", "qa", "kw", "bh", "om",
        "jo", "lb", "sy", "iq", "ps", "ye", "sd", "fr", "it", "de", "gb", "us",
    )

    fun name(code: String): String =
        Locale("", code.uppercase()).getDisplayCountry(Locale.ENGLISH).ifBlank { code.uppercase() }

    /** "tn" -> 🇹🇳 */
    fun flag(code: String): String = buildString {
        code.uppercase().forEach { c -> if (c in 'A'..'Z') appendCodePoint(0x1F1E6 + (c - 'A')) }
    }

    fun label(code: String) = "${flag(code)} ${name(code)}"

    fun sort(codes: Collection<String>): List<String> {
        val (pinned, rest) = codes.partition { it in PRIORITY }
        return pinned.sortedBy { PRIORITY.indexOf(it) } + rest.sortedBy { name(it) }
    }
}
