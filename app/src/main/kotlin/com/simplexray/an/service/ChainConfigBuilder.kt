package com.simplexray.an.service

import org.json.JSONArray
import org.json.JSONObject

/** Routes Xray outbounds through the SSH engine's local SOCKS5 listener. */
object ChainConfigBuilder {
    const val DEFAULT_SSH_SOCKS_TAG = "ssh-chain-upstream"

    @JvmStatic
    @JvmOverloads
    fun applySshUpstream(
        source: String,
        host: String = "127.0.0.1",
        port: Int = 1080,
        upstreamTag: String = DEFAULT_SSH_SOCKS_TAG
    ): String {
        require(host.isNotBlank()) { "SOCKS host is required" }
        require(port in 1..65535) { "SOCKS port must be between 1 and 65535" }
        require(upstreamTag.isNotBlank()) { "Upstream tag is required" }
        val root = JSONObject(source)
        val original = root.optJSONArray("outbounds") ?: JSONArray()
        require(original.length() > 0) { "Xray config has no outbounds" }
        for (i in 0 until original.length()) {
            require(original.optJSONObject(i)?.optString("tag") != upstreamTag) {
                "Outbound tag '$upstreamTag' is reserved for SSH chain"
            }
        }
        val socks = JSONObject().put("tag", upstreamTag).put("protocol", "socks")
            .put("settings", JSONObject().put("servers", JSONArray().put(
                JSONObject().put("address", host).put("port", port)
            )))
        val chained = JSONArray().put(socks)
        for (i in 0 until original.length()) {
            val outbound = original.optJSONObject(i) ?: continue
            if (outbound.optString("protocol") != "blackhole" && outbound.optString("tag") != "api") {
                outbound.put("proxySettings", JSONObject().put("tag", upstreamTag))
            }
            chained.put(outbound)
        }
        root.put("outbounds", chained)
        return root.toString(2)
    }

    /** Converts each configured Xray egress to the SSH SOCKS upstream while preserving routing tags. */
    @JvmStatic
    fun applySshOnly(source: String, port: Int = 1080): String {
        require(port in 1..65535)
        val root = JSONObject(source)
        val existing = root.optJSONArray("outbounds") ?: JSONArray()
        val rewritten = JSONArray()
        val server = JSONArray().put(JSONObject().put("address", "127.0.0.1").put("port", port))
        for (i in 0 until existing.length()) {
            val outbound = existing.optJSONObject(i) ?: continue
            if (outbound.optString("protocol") != "blackhole" && outbound.optString("tag") != "api") {
                outbound.put("protocol", "socks")
                outbound.put("settings", JSONObject().put("servers", server))
                outbound.remove("streamSettings")
                outbound.remove("proxySettings")
            }
            rewritten.put(outbound)
        }
        if (rewritten.length() == 0) {
            rewritten.put(JSONObject().put("tag", "ssh-egress").put("protocol", "socks")
                .put("settings", JSONObject().put("servers", server)))
        }
        root.put("outbounds", rewritten)
        return root.toString(2)
    }
}
