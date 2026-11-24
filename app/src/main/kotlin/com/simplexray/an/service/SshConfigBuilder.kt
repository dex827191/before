package com.simplexray.an.service

import org.json.JSONArray
import org.json.JSONObject

/** Rewrites Xray egress to the SSH engine's local SOCKS5 upstream. */
object SshConfigBuilder {
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
