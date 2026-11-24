package com.simplexray.an.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mdevz.sp.core.model.PayloadMode
import com.simplexray.an.service.ProtocolMode
import com.simplexray.an.service.ProtocolSettings

@Composable
fun ProfilesScreen() {
    val context = LocalContext.current.applicationContext
    val saved = ProtocolSettings.sshProfileFields(context)
    var mode by rememberSaveable { mutableStateOf(ProtocolSettings.mode(context).name) }
    var host by rememberSaveable { mutableStateOf(saved?.sshHost.orEmpty()) }
    var port by rememberSaveable { mutableStateOf((saved?.sshPort ?: 22).toString()) }
    var username by rememberSaveable { mutableStateOf(saved?.username.orEmpty()) }
    var password by rememberSaveable { mutableStateOf(saved?.password.orEmpty()) }
    var payloadEnabled by rememberSaveable { mutableStateOf(saved?.payloadEnabled ?: false) }
    var payloadMode by rememberSaveable { mutableStateOf(saved?.payloadMode ?: PayloadMode.NORMAL) }
    var payload by rememberSaveable { mutableStateOf(saved?.payload.orEmpty()) }
    var proxyEnabled by rememberSaveable { mutableStateOf(saved?.proxyEnabled ?: false) }
    var proxyHost by rememberSaveable { mutableStateOf(saved?.proxyHost.orEmpty()) }
    var proxyPort by rememberSaveable { mutableStateOf((saved?.proxyPort ?: 8080).toString()) }
    var tlsEnabled by rememberSaveable { mutableStateOf(saved?.tlsEnabled ?: false) }
    var sni by rememberSaveable { mutableStateOf(saved?.sni.orEmpty()) }
    var tlsVersion by rememberSaveable { mutableStateOf(saved?.tlsVersion ?: "default") }
    var message by rememberSaveable { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pilih Protokol", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (mode == ProtocolMode.XRAY.name) Button(onClick = { mode = ProtocolMode.XRAY.name; ProtocolSettings.setMode(context, ProtocolMode.XRAY) }) { Text("Xray") }
                        else OutlinedButton(onClick = { mode = ProtocolMode.XRAY.name; ProtocolSettings.setMode(context, ProtocolMode.XRAY) }) { Text("Xray") }
                        if (mode == ProtocolMode.SSH.name) Button(onClick = { mode = ProtocolMode.SSH.name; ProtocolSettings.setMode(context, ProtocolMode.SSH) }) { Text("SSH") }
                        else OutlinedButton(onClick = { mode = ProtocolMode.SSH.name; ProtocolSettings.setMode(context, ProtocolMode.SSH) }) { Text("SSH") }
                    }
                    Text(if (mode == ProtocolMode.XRAY.name) "Xray menggunakan konfigurasi JSON yang dipilih pada tab Konfigurasi." else "SSH menggunakan profil SSH di bawah sebagai koneksi SOCKS5 upstream untuk tunnel.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (mode == ProtocolMode.SSH.name) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Profil SSH", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text("SSH Host / IP") }, singleLine = true)
                        OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("SSH Port") }, singleLine = true)
                        OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(payloadEnabled, { payloadEnabled = it }); Text("Aktifkan Payload") }
                        if (payloadEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (payloadMode == PayloadMode.NORMAL) Button(onClick = { payloadMode = PayloadMode.NORMAL }) { Text("Normal") } else OutlinedButton(onClick = { payloadMode = PayloadMode.NORMAL }) { Text("Normal") }
                                if (payloadMode == PayloadMode.ENHANCED) Button(onClick = { payloadMode = PayloadMode.ENHANCED }) { Text("Enhanced") } else OutlinedButton(onClick = { payloadMode = PayloadMode.ENHANCED }) { Text("Enhanced") }
                            }
                            OutlinedTextField(payload, { payload = it }, Modifier.fillMaxWidth(), label = { Text("Payload") }, minLines = 4, maxLines = 8)
                            Text("Placeholder: [host], [port], [host_port], [cr], [lf], [crlf], [ua].", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(proxyEnabled, { proxyEnabled = it }); Text("Gunakan HTTP Proxy") }
                        if (proxyEnabled) {
                            OutlinedTextField(proxyHost, { proxyHost = it }, Modifier.fillMaxWidth(), label = { Text("Proxy Host") }, singleLine = true)
                            OutlinedTextField(proxyPort, { proxyPort = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("Proxy Port") }, singleLine = true)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(tlsEnabled, { tlsEnabled = it }); Text("Aktifkan TLS / SSL") }
                        if (tlsEnabled) {
                            OutlinedTextField(sni, { sni = it }, Modifier.fillMaxWidth(), label = { Text("SNI / TLS Host") }, singleLine = true)
                            Text("TLS Version", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("default", "TLSv1.2", "TLSv1.3").forEach { version ->
                                    if (tlsVersion == version) Button(onClick = { tlsVersion = version }) { Text(version) } else OutlinedButton(onClick = { tlsVersion = version }) { Text(version) }
                                }
                            }
                        }
                        Button(onClick = {
                            runCatching {
                                val sshPort = port.toIntOrNull() ?: error("Port SSH harus angka")
                                val httpPort = if (proxyEnabled) proxyPort.toIntOrNull() ?: error("Proxy port harus angka") else 0
                                ProtocolSettings.saveSshFields(context, host, sshPort, username, password, payloadEnabled, payloadMode, payload, proxyEnabled, proxyHost, httpPort, tlsEnabled, sni, tlsVersion)
                                ProtocolSettings.setMode(context, ProtocolMode.SSH)
                                mode = ProtocolMode.SSH.name
                            }.onSuccess { message = "Profil SSH berhasil disimpan" }
                                .onFailure { message = it.message ?: "Profil SSH tidak valid" }
                        }, Modifier.fillMaxWidth()) { Text("Simpan Profil SSH") }
                        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
