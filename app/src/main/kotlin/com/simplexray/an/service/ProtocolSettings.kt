package com.simplexray.an.service

import android.content.Context
import com.mdevz.sp.core.model.SshProfile
import com.mdevz.sp.core.model.PayloadMode
import com.mdevz.sp.core.config.ProfileUriCodec

object ProtocolSettings {
    private const val PREFS = "multi_protocol_settings"
    private const val KEY_MODE = "mode"
    private const val KEY_SSH_URI = "ssh_profile_uri"
    private const val KEY_SSH_READY = "ssh_upstream_ready"
    private const val KEY_SSH_SOCKS_PORT = "ssh_socks_port"

    fun mode(context: Context): ProtocolMode = runCatching {
        ProtocolMode.valueOf(prefs(context).getString(KEY_MODE, ProtocolMode.XRAY.name)!!)
    }.getOrDefault(ProtocolMode.XRAY)

    fun setMode(context: Context, mode: ProtocolMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun sshProfileUri(context: Context): String =
        prefs(context).getString(KEY_SSH_URI, "").orEmpty()

    fun sshLocalPort(context: Context): Int =
        prefs(context).getInt(KEY_SSH_SOCKS_PORT, 1080).coerceIn(1, 65535)

    fun setSshLocalPort(context: Context, port: Int) {
        require(port in 1..65535)
        prefs(context).edit().putInt(KEY_SSH_SOCKS_PORT, port).apply()
    }

    fun sshUpstreamReady(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SSH_READY, false)

    fun setSshUpstreamReady(context: Context, ready: Boolean) {
        prefs(context).edit().putBoolean(KEY_SSH_READY, ready).apply()
    }

    fun setSshProfileUri(context: Context, uri: String) {
        require(uri.startsWith("sp://")) { "SSH profile URI must start with sp://" }
        // Validate before storing; the UI now collects fields instead of asking users to paste a URI.
        ProfileUriCodec.decode(uri.trim())
        prefs(context).edit().putString(KEY_SSH_URI, uri.trim()).apply()
    }

    fun saveSshFields(
        context: Context,
        host: String,
        port: Int,
        username: String,
        password: String,
        payloadEnabled: Boolean = false,
        payloadMode: PayloadMode = PayloadMode.NORMAL,
        payload: String = "",
        proxyEnabled: Boolean = false,
        proxyHost: String = "",
        proxyPort: Int = 0,
        tlsEnabled: Boolean = false,
        sni: String = "",
        tlsVersion: String = "default"
    ) {
        val profile = SshProfile(
            name = "SimpleXray SSH",
            sshHost = host.trim(),
            sshPort = port,
            username = username.trim(),
            password = password,
            payloadEnabled = payloadEnabled,
            payloadMode = payloadMode,
            payload = payload,
            proxyEnabled = proxyEnabled,
            proxyHost = proxyHost.trim(),
            proxyPort = proxyPort,
            tlsEnabled = tlsEnabled,
            sni = sni.trim(),
            tlsVersion = tlsVersion,
            localSocksPort = sshLocalPort(context)
        )
        val encoded = ProfileUriCodec.encode(profile)
        prefs(context).edit().putString(KEY_SSH_URI, encoded).apply()
    }

    fun sshProfileFields(context: Context): SshProfile? =
        sshProfileUri(context).takeIf { it.startsWith("sp://") }?.let {
            runCatching { ProfileUriCodec.decode(it) }.getOrNull()
        }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
