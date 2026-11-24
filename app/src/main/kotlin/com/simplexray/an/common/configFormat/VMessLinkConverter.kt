package com.simplexray.an.common.configFormat

import android.content.Context
import android.util.Base64
import com.simplexray.an.common.FilenameValidator
import com.simplexray.an.prefs.Preferences
import org.json.JSONArray
import org.json.JSONObject

/** Converts a standard vmess:// Base64 JSON share link into an Xray JSON config. */
class VMessLinkConverter : ConfigFormatConverter {
    override fun detect(content: String): Boolean = content.trimStart().startsWith("vmess://", ignoreCase = true)

    override fun convert(context: Context, content: String): Result<DetectedConfig> = runCatching {
        val encoded = content.trim().substringAfter("vmess://", "")
        require(encoded.isNotBlank()) { "VMess link has no Base64 payload" }
        val normalized = encoded.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val decoded = String(Base64.decode(padded, Base64.DEFAULT), Charsets.UTF_8)
        val source = JSONObject(decoded)

        val address = source.optString("add").trim()
        val port = source.optString("port").toIntOrNull()
            ?: source.optInt("port", 0)
        val id = source.optString("id").trim()
        require(address.isNotBlank()) { "VMess server address is missing" }
        require(port in 1..65535) { "VMess server port is invalid" }
        require(id.isNotBlank()) { "VMess UUID is missing" }

        val network = source.optString("net", "tcp").lowercase().ifBlank { "tcp" }
        val tlsEnabled = source.optString("tls").equals("tls", ignoreCase = true)
        val host = source.optString("host").ifBlank { source.optString("sni").ifBlank { address } }
        val path = source.optString("path").ifBlank { "/" }
        val nameCandidate = source.optString("ps").ifBlank { "imported_vmess_${System.currentTimeMillis()}" }
        val name = nameCandidate.map { character ->
            if (character in "\\/:*?\"<>|") '_' else character
        }.joinToString("").trim().ifBlank { "imported_vmess_${System.currentTimeMillis()}" }
        require(FilenameValidator.validateFilename(context, name) == null) { "Invalid VMess profile name" }

        val user = JSONObject()
            .put("id", id)
            .put("alterId", source.optString("aid", "0").toIntOrNull() ?: 0)
            .put("security", source.optString("scy").ifBlank { "auto" })
        val outbound = JSONObject()
            .put("protocol", "vmess")
            .put("settings", JSONObject().put("vnext", JSONArray().put(
                JSONObject()
                    .put("address", address)
                    .put("port", port)
                    .put("users", JSONArray().put(user))
            )))

        val streamSettings = JSONObject().put("network", network)
            .put("security", if (tlsEnabled) "tls" else "none")
        when (network) {
            "ws" -> {
                val headers = JSONObject()
                if (host.isNotBlank()) headers.put("Host", host)
                streamSettings.put("wsSettings", JSONObject().put("path", path).put("headers", headers))
            }
            "grpc" -> streamSettings.put("grpcSettings", JSONObject().put("serviceName", path.trim('/')))
            "httpupgrade" -> streamSettings.put("httpupgradeSettings", JSONObject().put("host", host).put("path", path))
        }
        if (tlsEnabled) {
            val tls = JSONObject()
                .put("serverName", source.optString("sni").ifBlank { host })
                .put("allowInsecure", false)
            val alpn = source.optString("alpn").split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (alpn.isNotEmpty()) tls.put("alpn", JSONArray(alpn))
            source.optString("fp").takeIf { it.isNotBlank() }?.let { tls.put("fingerprint", it) }
            streamSettings.put("tlsSettings", tls)
        }
        outbound.put("streamSettings", streamSettings)

        val config = JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("inbounds", JSONArray().put(
                JSONObject()
                    .put("listen", "127.0.0.1")
                    .put("port", Preferences(context).socksPort)
                    .put("protocol", "socks")
                    .put("settings", JSONObject().put("udp", true))
            ))
            .put("outbounds", JSONArray().put(outbound))

        name to config.toString(2)
    }
}
