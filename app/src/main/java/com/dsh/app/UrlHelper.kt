package com.dsh.app

import java.net.URI

object UrlHelper {
    const val DEFAULT_URL = "http://192.168.0.102:3080/"

    /**
     * Normalizes the user input into a valid web URL.
     * Prepends "http://" if no scheme is provided, and ensures trailing slash when appropriate.
     */
    fun normalize(rawInput: String?): String {
        if (rawInput.isNullOrBlank()) {
            return DEFAULT_URL
        }

        var trimmed = rawInput.trim()

        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed = "http://$trimmed"
        }

        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: ""
            val port = if (uri.port != -1) ":${uri.port}" else ""
            var path = uri.rawPath ?: ""
            val query = if (uri.rawQuery != null) "?${uri.rawQuery}" else ""
            val fragment = if (uri.rawFragment != null) "#${uri.rawFragment}" else ""

            if (path.isEmpty()) {
                path = "/"
            }

            "$scheme://$host$port$path$query$fragment"
        } catch (_: Exception) {
            if (!trimmed.endsWith("/")) "$trimmed/" else trimmed
        }
    }

    /**
     * Checks if the given string is a valid HTTP/HTTPS URL with a non-empty host.
     */
    fun isValid(rawInput: String?): Boolean {
        if (rawInput.isNullOrBlank()) return false
        val normalized = normalize(rawInput)
        return try {
            val uri = URI(normalized)
            (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
                    !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
