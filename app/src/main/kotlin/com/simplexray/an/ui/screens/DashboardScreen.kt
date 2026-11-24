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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
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
fun DashboardScreen(mainViewModel: MainViewModel) {
    val coreStats by mainViewModel.coreStatsState.collectAsState()
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Traffic", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    StatRow(stringResource(R.string.stats_uplink), formatBytes(coreStats.uplink))
                    StatRow(stringResource(R.string.stats_downlink), formatBytes(coreStats.downlink))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Stats", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    StatRow(stringResource(R.string.stats_num_goroutine), formatNumber(coreStats.numGoroutine.toLong()))
                    StatRow(stringResource(R.string.stats_num_gc), formatNumber(coreStats.numGC.toLong()))
                    StatRow(stringResource(R.string.stats_alloc), formatBytes(coreStats.alloc))
                    StatRow(stringResource(R.string.stats_uptime), formatUptime(coreStats.uptime))
                }
            }
        }
    }
}


@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
