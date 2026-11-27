package com.lumacraft.studio.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumacraft.studio.model.ExportFormat
import com.lumacraft.studio.ui.components.LumaSliderRow
import com.lumacraft.studio.ui.components.LumaSwitchRow
import com.lumacraft.studio.ui.components.SectionCard
import com.lumacraft.studio.vm.StudioViewModel

@Composable
fun BatchScreen(vm: StudioViewModel) {
    val st by vm.ui.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
            }
            vm.addBatchPhotos(uris)
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton(onClick = { vm.goScreen(com.lumacraft.studio.model.Screen.HOME) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            Text("Batch Editing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            "Apply one look to many photos and export them all. Overlays, drawing and tap fixes are per-photo tools and are not applied in batch.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.padding(horizontal = 20.dp)) {
            Button(onClick = {
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Rounded.AddAPhoto, null, modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select photos (${st.batch.uris.size})")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionCard("Options") {
                LumaSwitchRow(
                    "Apply current editor look",
                    "Uses the grade currently loaded in the editor",
                    st.batch.applyCurrentLook
                ) { enabled -> vm.updateBatch { it.copy(applyCurrentLook = enabled) } }
                Text("Output format", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    ExportFormat.entries.forEach { f ->
                        val sel = st.batch.format == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { vm.updateBatch { it.copy(format = f) } }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                f.label,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                LumaSliderRow("Quality", st.batch.quality.toFloat(), 90f, 1f..100f, onChange = { q -> vm.updateBatch { it.copy(quality = q.toInt()) } })
                Text("Max resolution", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                    listOf(1080, 2048, 4096).forEach { d ->
                        val sel = st.batch.maxDim == d
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { vm.updateBatch { it.copy(maxDim = d) } }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${d} px",
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            if (st.batch.uris.isNotEmpty()) {
                SectionCard("Queue — ${st.batch.uris.size} photos") {
                    st.batch.uris.forEach { uri ->
                        Text(
                            "• ${uri.lastPathSegment ?: uri}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (st.batchRunning) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(
                        "Processing ${st.batchDone} / ${st.batch.uris.size}…",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (st.batch.uris.isEmpty()) 0f else st.batchDone.toFloat() / st.batch.uris.size },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = { vm.cancelBatch() }) { Text("Cancel") }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Button(
                onClick = { vm.startBatch() },
                enabled = st.batch.uris.isNotEmpty() && !st.batchRunning,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (st.batchRunning) "Processing…" else "Export all ${st.batch.uris.size} photos", fontWeight = FontWeight.Bold)
            }
        }
    }
}
