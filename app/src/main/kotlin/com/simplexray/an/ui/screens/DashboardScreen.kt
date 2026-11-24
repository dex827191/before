package com.simplexray.an.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.simplexray.an.service.ProtocolMode
import com.simplexray.an.service.ProtocolSettings
import com.mdevz.sp.core.model.PayloadMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.simplexray.an.R
import com.simplexray.an.common.formatBytes
import com.simplexray.an.common.formatNumber
import com.simplexray.an.common.formatUptime
import com.simplexray.an.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    mainViewModel: MainViewModel
) {
    val coreStats by mainViewModel.coreStatsState.collectAsState()
    val appContext = LocalContext.current.applicationContext
    var selectedMode by rememberSaveable {
        mutableStateOf(ProtocolSettings.mode(appContext).name)
    }
    val savedSsh = ProtocolSettings.sshProfileFields(appContext)
    var sshHost by rememberSaveable { mutableStateOf(savedSsh?.sshHost.orEmpty()) }
    var sshPort by rememberSaveable { mutableStateOf((savedSsh?.sshPort ?: 22).toString()) }
    var sshUsername by rememberSaveable { mutableStateOf(savedSsh?.username.orEmpty()) }
    var sshPassword by rememberSaveable { mutableStateOf(savedSsh?.password.orEmpty()) }
    var payloadEnabled by rememberSaveable { mutableStateOf(savedSsh?.payloadEnabled ?: false) }
    var payloadMode by rememberSaveable { mutableStateOf(savedSsh?.payloadMode ?: PayloadMode.NORMAL) }
    var payload by rememberSaveable { mutableStateOf(savedSsh?.payload.orEmpty()) }
    var proxyEnabled by rememberSaveable { mutableStateOf(savedSsh?.proxyEnabled ?: false) }
    var proxyHost by rememberSaveable { mutableStateOf(savedSsh?.proxyHost.orEmpty()) }
    var proxyPort by rememberSaveable { mutableStateOf((savedSsh?.proxyPort ?: 8080).toString()) }
    var tlsEnabled by rememberSaveable { mutableStateOf(savedSsh?.tlsEnabled ?: false) }
    var sni by rememberSaveable { mutableStateOf(savedSsh?.sni.orEmpty()) }
    var tlsVersion by rememberSaveable { mutableStateOf(savedSsh?.tlsVersion ?: "default") }
    var profileMessage by rememberSaveable { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                mainViewModel.updateCoreStats()
                delay(1000)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Protocol mode", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProtocolMode.entries.forEach { mode ->
                            if (selectedMode == mode.name) {
                                Button(onClick = { selectedMode = mode.name; ProtocolSettings.setMode(appContext, mode) }) {
                                    Text(mode.name)
                                }
                            } else {
                                OutlinedButton(onClick = { selectedMode = mode.name; ProtocolSettings.setMode(appContext, mode) }) {
                                    Text(mode.name)
                                }
                            }
                        }
                    }
                    if (selectedMode != ProtocolMode.XRAY.name) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = sshHost,
                            onValueChange = { sshHost = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("SSH Host / IP") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sshPort,
                            onValueChange = { sshPort = it.filter(Char::isDigit).take(5) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("SSH Port") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sshUsername,
                            onValueChange = { sshUsername = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Username") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sshPassword,
                            onValueChange = { sshPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = payloadEnabled, onCheckedChange = { payloadEnabled = it })
                            Text("Aktifkan Payload")
                        }
                        if (payloadEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (payloadMode == PayloadMode.NORMAL) Button(onClick = { payloadMode = PayloadMode.NORMAL }) { Text("Normal") }
                                else OutlinedButton(onClick = { payloadMode = PayloadMode.NORMAL }) { Text("Normal") }
                                if (payloadMode == PayloadMode.ENHANCED) Button(onClick = { payloadMode = PayloadMode.ENHANCED }) { Text("Enhanced") }
                                else OutlinedButton(onClick = { payloadMode = PayloadMode.ENHANCED }) { Text("Enhanced") }
                            }
                            OutlinedTextField(
                                value = payload, onValueChange = { payload = it },
                                modifier = Modifier.fillMaxWidth(), label = { Text("Payload") },
                                minLines = 4, maxLines = 8
                            )
                            Text("Placeholder: [host], [port], [host_port], [cr], [lf], [crlf], [ua].", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = proxyEnabled, onCheckedChange = { proxyEnabled = it })
                            Text("Gunakan HTTP Proxy")
                        }
                        if (proxyEnabled) {
                            OutlinedTextField(value = proxyHost, onValueChange = { proxyHost = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Proxy Host") }, singleLine = true)
                            OutlinedTextField(value = proxyPort, onValueChange = { proxyPort = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), label = { Text("Proxy Port") }, singleLine = true)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = tlsEnabled, onCheckedChange = { tlsEnabled = it })
                            Text("Aktifkan TLS / SSL")
                        }
                        if (tlsEnabled) {
                            OutlinedTextField(value = sni, onValueChange = { sni = it }, modifier = Modifier.fillMaxWidth(), label = { Text("SNI / TLS Host") }, singleLine = true)
                            Text("TLS Version", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("default", "TLSv1.2", "TLSv1.3").forEach { version ->
                                    if (tlsVersion == version) Button(onClick = { tlsVersion = version }) { Text(version) }
                                    else OutlinedButton(onClick = { tlsVersion = version }) { Text(version) }
                                }
                            }
                        }
                        Button(onClick = {
                            runCatching {
                                val port = sshPort.toIntOrNull() ?: error("Port SSH harus angka")
                                val pPort = if (proxyEnabled) proxyPort.toIntOrNull() ?: error("Proxy port harus angka") else 0
                                ProtocolSettings.saveSshFields(
                                    appContext, sshHost, port, sshUsername, sshPassword,
                                    payloadEnabled, payloadMode, payload, proxyEnabled, proxyHost, pPort,
                                    tlsEnabled, sni, tlsVersion
                                )
                            }.onSuccess { profileMessage = "Profil SSH tersimpan" }
                             .onFailure { profileMessage = it.message ?: "Data SSH tidak valid" }
                        }) { Text("Simpan profil SSH") }
                        if (profileMessage.isNotBlank()) Text(profileMessage, style = MaterialTheme.typography.bodySmall)
                        Text("CHAIN memakai koneksi SSH sebagai upstream SOCKS5 untuk outbound Xray.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Traffic",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    StatRow(
                        label = stringResource(id = R.string.stats_uplink),
                        value = formatBytes(coreStats.uplink)
                    )
                    StatRow(
                        label = stringResource(id = R.string.stats_downlink),
                        value = formatBytes(coreStats.downlink)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Stats",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    StatRow(
                        label = stringResource(id = R.string.stats_num_goroutine),
                        value = formatNumber(coreStats.numGoroutine.toLong())
                    )
                    StatRow(
                        label = stringResource(id = R.string.stats_num_gc),
                        value = formatNumber(coreStats.numGC.toLong())
                    )
                    StatRow(
                        label = stringResource(id = R.string.stats_alloc),
                        value = formatBytes(coreStats.alloc)
                    )
                    StatRow(
                        label = stringResource(id = R.string.stats_uptime),
                        value = formatUptime(coreStats.uptime)
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}