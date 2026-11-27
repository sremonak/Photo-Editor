package com.lumacraft.studio.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumacraft.studio.model.BUILT_IN_PRESETS
import com.lumacraft.studio.ui.components.LumaTextInputDialog
import com.lumacraft.studio.vm.StudioViewModel

@Composable
fun PresetsScreen(vm: StudioViewModel) {
    val st by vm.ui.collectAsState()
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameInitial by remember { mutableStateOf("") }
    var showSave by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val target = exportPresetRef ?: return@rememberLauncherForActivityResult
            val json = vm.exportPresetJson(target)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray())
            }
        }
        exportPresetRef = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            if (json != null) vm.importPreset(json)
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            IconButton(onClick = { vm.goScreen(com.lumacraft.studio.model.Screen.HOME) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            Text("Preset Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            "Presets store the full edit recipe (non-destructive). Import & export as JSON files.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.padding(horizontal = 20.dp)) {
            Button(onClick = {
                if (st.source != null) showSave = true
                else vm.notify("Open a photo first, craft a look, then save it as a preset")
            }) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save current look")
            }
            Spacer(modifier = Modifier.width(10.dp))
            TextButton(onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream"))
            }) {
                Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import JSON")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                Text(
                    "Built-in Pro Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(BUILT_IN_PRESETS.size) { i ->
                val (name, state) = BUILT_IN_PRESETS[i]
                PresetRow(
                    name = name,
                    subtitle = describe(state),
                    onApply = {
                        if (st.source != null) vm.applyBuiltInPreset(name, state)
                        else vm.notify("Open a photo first, then apply this preset")
                    },
                    trailing = {}
                )
            }
            item {
                Text(
                    "My Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            if (st.userPresets.isEmpty()) {
                item {
                    Text(
                        "No custom presets yet. Open a photo, craft a look, then \"Save current look\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            items(st.userPresets.size) { i ->
                val p = st.userPresets[i]
                PresetRow(
                    name = p.name,
                    subtitle = describe(p.state),
                    onApply = {
                        if (st.source != null) vm.applyPreset(p)
                    },
                    trailing = {
                        IconButton(onClick = { renameTarget = p.id; renameInitial = p.name }) {
                            Icon(Icons.Rounded.Edit, "Rename", modifier = Modifier.height(17.dp))
                        }
                        IconButton(onClick = {
                            exportPresetRef = p
                            exportLauncher.launch("LumaCraft_Preset_${p.name}.json")
                        }) {
                            Icon(Icons.Rounded.Download, "Export JSON", modifier = Modifier.height(17.dp))
                        }
                        IconButton(onClick = { vm.deletePreset(p.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, "Delete", modifier = Modifier.height(17.dp))
                        }
                    }
                )
            }
        }
    }

    if (showSave) {
        LumaTextInputDialog(
            title = "Preset name",
            initial = "",
            onDismiss = { showSave = false },
            onConfirm = { name ->
                vm.savePreset(name)
                showSave = false
            }
        )
    }
    if (renameTarget != null) {
        LumaTextInputDialog(
            title = "Rename preset",
            initial = renameInitial,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                vm.renamePreset(renameTarget!!, name)
                renameTarget = null
            }
        )
    }
}

private var exportPresetRef: com.lumacraft.studio.model.PresetItem? = null

private fun describe(s: com.lumacraft.studio.model.EditState): String {
    val a = s.adjust
    val parts = mutableListOf<String>()
    if (a.contrast != 0f) parts.add("con ${a.contrast.toInt()}")
    if (a.temperature != 0f) parts.add("temp ${a.temperature.toInt()}")
    if (a.saturation != 0f) parts.add("sat ${a.saturation.toInt()}")
    if (a.fade > 0f) parts.add("fade ${a.fade.toInt()}")
    if (a.grain > 0f) parts.add("grain ${a.grain.toInt()}")
    if (s.skinSmooth > 0f) parts.add("skin ${s.skinSmooth.toInt()}")
    if (s.split.shadowSat > 0f || s.split.highlightSat > 0f) parts.add("split-tone")
    return if (parts.isEmpty()) "clean grade" else parts.joinToString(" · ")
}

@Composable
private fun PresetRow(
    name: String,
    subtitle: String,
    onApply: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable { onApply() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}
