package com.lumacraft.studio.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lumacraft.studio.model.COLLAGE_ASPECTS
import com.lumacraft.studio.model.COLLAGE_BG_COLORS
import com.lumacraft.studio.model.COLLAGE_TEMPLATES
import com.lumacraft.studio.ui.components.BusyOverlay
import com.lumacraft.studio.ui.components.LumaSliderRow
import com.lumacraft.studio.ui.components.LumaTextInputDialog
import com.lumacraft.studio.vm.StudioViewModel

@Composable
fun CollageScreen(vm: StudioViewModel) {
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
            vm.addCollagePhotos(uris)
        }
    }

    var showCaption by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            IconButton(onClick = { vm.goScreen(com.lumacraft.studio.model.Screen.HOME) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
            }
            Text("Collage Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = {
                pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Rounded.AddAPhoto, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add photos")
            }
        }

        if (st.collage.photos.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.AddAPhoto, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pick 2–9 photos to compose a collage", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .pointerInput(st.collageActiveCell) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            vm.panCollageCell(
                                -(change.position.x - change.previousPosition.x) / size.width,
                                -(change.position.y - change.previousPosition.y) / size.height
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val pv = st.collagePreview
                if (pv != null) {
                    androidx.compose.foundation.Image(
                        bitmap = pv,
                        contentDescription = "Collage preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                if (st.busy != null) BusyOverlay(st.busy!!)
            }
            Text(
                "Drag on the preview to reposition the selected cell (${st.collageActiveCell + 1})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // cell selector
        if (st.collage.photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                items(st.collage.photos.size) { i ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (st.collageActiveCell == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { vm.setCollageActiveCell(i) }
                    ) {
                        Text("${i + 1}", color = if (st.collageActiveCell == i) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                "Layout — ${COLLAGE_TEMPLATES.size} templates",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                items(COLLAGE_TEMPLATES.size) { i ->
                    TemplateThumb(
                        cells = COLLAGE_TEMPLATES[i].cells,
                        selected = st.collage.templateIdx == i,
                        onClick = { vm.updateCollage { it.copy(templateIdx = i) } }
                    )
                }
            }
            LumaSliderRow("Spacing", st.collage.spacingRel * 100f, 2f, 0f..10f, onChange = { vm.updateCollage { c -> c.copy(spacingRel = it / 100f) } })
            LumaSliderRow("Corner radius", st.collage.cornerRel * 100f, 0f, 0f..8f, onChange = { vm.updateCollage { c -> c.copy(cornerRel = it / 100f) } })
            Text(
                "Background color",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                COLLAGE_BG_COLORS.forEach { c ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(c.toInt()))
                            .border(2.dp, if (st.collage.bgColor == c) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { vm.updateCollage { it.copy(bgColor = c) } }
                    )
                }
            }
            Text(
                "Aspect ratio",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                COLLAGE_ASPECTS.forEach { a ->
                    val sel = st.collage.aspect == a
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { vm.updateCollage { it.copy(aspect = a) } }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(a, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedButton(onClick = { showCaption = true }) { Text("Caption") }
                Spacer(modifier = Modifier.width(10.dp))
                Button(onClick = { vm.exportCollage() }, enabled = st.collage.photos.isNotEmpty()) { Text("Save collage") }
            }
        }
    }

    if (showCaption) {
        LumaTextInputDialog("Collage caption", st.collage.caption, onDismiss = { showCaption = false }, onConfirm = {
            vm.updateCollage { c -> c.copy(caption = it) }
            showCaption = false
        })
    }
}

@Composable
private fun TemplateThumb(cells: List<android.graphics.RectF>, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
            cells.forEach { cell ->
                drawRoundRect(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    topLeft = Offset(cell.left * size.width, cell.top * size.height),
                    size = Size(cell.width() * size.width, cell.height() * size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                    style = Stroke(2f)
                )
            }
        }
    }
}
