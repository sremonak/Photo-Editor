package com.lumacraft.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumacraft.studio.BuildConfig
import com.lumacraft.studio.model.ExportFormat
import com.lumacraft.studio.ui.components.LumaSliderRow
import com.lumacraft.studio.ui.components.LumaSwitchRow
import com.lumacraft.studio.ui.components.SectionCard
import com.lumacraft.studio.vm.StudioViewModel

@Composable
fun SettingsScreen(vm: StudioViewModel) {
    val st by vm.ui.collectAsState()
    var showLicenses by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton(onClick = { vm.goScreen(com.lumacraft.studio.model.Screen.HOME) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionCard("Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    listOf("System", "Light", "Dark").forEach { mode ->
                        val sel = st.settings.themeMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { vm.updateSettings { it.copy(themeMode = mode) } }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                mode,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                LumaSwitchRow("Haptic feedback", "Subtle vibrations on tool interactions", st.settings.haptics) { enabled ->
                    vm.updateSettings { it.copy(haptics = enabled) }
                }
            }

            SectionCard("Export defaults") {
                Text("Default format", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    ExportFormat.entries.forEach { f ->
                        val sel = st.settings.defaultFormat == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { vm.updateSettings { it.copy(defaultFormat = f) } }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                f.label,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                LumaSliderRow("Default quality", st.settings.defaultQuality.toFloat(), 92f, 1f..100f, onChange = { q -> vm.updateSettings { it.copy(defaultQuality = q.toInt()) } })
                LumaSwitchRow("Strip EXIF by default", "Privacy-first export behavior", st.settings.stripExifDefault) { enabled ->
                    vm.updateSettings { it.copy(stripExifDefault = enabled) }
                }
            }

            SectionCard("Storage") {
                Text(
                    "Edited photos save to Pictures/LumaCraft. Presets, sessions and settings are stored in the app's private storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
                OutlinedButton(
                    onClick = { vm.clearCache() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("Clear saved editing sessions") }
            }

            SectionCard("Privacy") {
                Text(
                    "LumaCraft has NO internet permission. Every photo, mask and AI operation runs entirely on your device. No account, no tracking, no analytics, no uploads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            SectionCard("About") {
                Text("LumaCraft Photo Studio v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    "Pure native Android · Kotlin · Jetpack Compose Material 3 · on-device ML Kit segmentation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
                TextButton(onClick = { showLicenses = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Open-source licenses")
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Open-source licenses") },
            text = {
                Column {
                    Text(
                        "Apache License 2.0:\n\n• Jetpack Compose & AndroidX (Google)\n• Coil (image loading)\n• ML Kit Selfie Segmentation (Google)\n• Kotlin Coroutines (JetBrains)\n\nFull license texts are included in the source repository.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } }
        )
    }
}
