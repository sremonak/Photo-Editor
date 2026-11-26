package com.luminapro.photoeditor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhotoFilter
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminapro.photoeditor.model.EditorState
import com.luminapro.photoeditor.model.EditorTool
import com.luminapro.photoeditor.model.HomeNavTab
import com.luminapro.photoeditor.model.LutPreset
import com.luminapro.photoeditor.model.RawProjectSample
import com.luminapro.photoeditor.model.SAMPLE_PROJECTS
import com.luminapro.photoeditor.model.STUDIO_LUTS
import com.luminapro.photoeditor.ui.components.drawProceduralRawScene
import com.luminapro.photoeditor.ui.theme.AmberBright
import com.luminapro.photoeditor.ui.theme.AmberGold
import com.luminapro.photoeditor.ui.theme.DarkroomBg
import com.luminapro.photoeditor.ui.theme.EmeraldOk
import com.luminapro.photoeditor.ui.theme.NeuralCyan
import com.luminapro.photoeditor.ui.theme.StudioBorder
import com.luminapro.photoeditor.ui.theme.StudioCard
import com.luminapro.photoeditor.ui.theme.StudioElevated
import com.luminapro.photoeditor.ui.theme.StudioSurface
import com.luminapro.photoeditor.ui.theme.TextMuted
import com.luminapro.photoeditor.ui.theme.TextPrimary
import com.luminapro.photoeditor.ui.theme.TextSecondary
import com.luminapro.photoeditor.ui.theme.VioletAi

@Composable
fun StudioHomeScreen(
    onPickGalleryPhoto: () -> Unit,
    onOpenSampleProject: (RawProjectSample, EditorTool) -> Unit,
    onOpenWithLut: (LutPreset) -> Unit
) {
    var activeTab by remember { mutableStateOf(HomeNavTab.STUDIO) }
    val previewDummyState = remember { EditorState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkroomBg)
            .statusBarsPadding()
    ) {
        // Top Native Studio Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF232736), Color(0xFF12151E))
                            )
                        )
                        .border(1.dp, AmberGold.copy(alpha = 0.55f), RoundedCornerShape(11.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Lumina Aperture",
                        tint = AmberBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LUMINA",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(AmberGold)
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "PRO",
                                color = DarkroomBg,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Text(
                        text = "16-BIT RAW DARKROOM ENGINE",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(StudioElevated)
                    .border(1.dp, StudioBorder, CircleShape)
                    .clickable { activeTab = HomeNavTab.ENGINE }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Engine Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Main Scrollable Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                HomeNavTab.STUDIO -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Hero Import & Quick Pro Actions Card
                        item {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 18.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF1D202C),
                                                Color(0xFF12141D)
                                            )
                                        )
                                    )
                                    .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Start New Master Grade",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Supports DNG, ARW, CR3, NEF, PNG & 16-bit TIFF",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Primary CTA: Open Photo / RAW
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(AmberGold, AmberBright)
                                            )
                                        )
                                        .clickable { onPickGalleryPhoto() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = DarkroomBg,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open Photo / Import RAW",
                                        color = DarkroomBg,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 4 Secondary Pro Studio Tiles
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QuickStudioTile(
                                        icon = Icons.Rounded.CameraAlt,
                                        label = "RAW Cam",
                                        accent = AmberBright,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenSampleProject(SAMPLE_PROJECTS[0], EditorTool.LIGHT) }
                                    )
                                    QuickStudioTile(
                                        icon = Icons.Rounded.ContentCut,
                                        label = "AI Cutout",
                                        accent = NeuralCyan,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenSampleProject(SAMPLE_PROJECTS[0], EditorTool.AI_LAB) }
                                    )
                                    QuickStudioTile(
                                        icon = Icons.Rounded.ColorLens,
                                        label = "HSL Lab",
                                        accent = VioletAi,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenSampleProject(SAMPLE_PROJECTS[1], EditorTool.HSL) }
                                    )
                                    QuickStudioTile(
                                        icon = Icons.Rounded.GridView,
                                        label = "Batch",
                                        accent = EmeraldOk,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenSampleProject(SAMPLE_PROJECTS[2], EditorTool.LUTS) }
                                    )
                                }
                            }
                        }

                        // AI Neural Suite Horizontal Strip
                        item {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = NeuralCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AI Neural Studio",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Text(
                                        text = "NPU ACCELERATED",
                                        color = NeuralCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val aiCards = listOf(
                                    Triple("3D Relight", "Studio Key-Light", Icons.Rounded.WbSunny),
                                    Triple("Bokeh f/1.2", "Depth Lens Map", Icons.Rounded.BlurOn),
                                    Triple("Magic Eraser", "Object Inpaint", Icons.Rounded.AutoFixHigh),
                                    Triple("4K Super-Res", "RAW Denoise", Icons.Rounded.Hd)
                                )

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 18.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(aiCards) { (title, subtitle, icon) ->
                                        Column(
                                            modifier = Modifier
                                                .width(134.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(StudioElevated)
                                                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    onOpenSampleProject(SAMPLE_PROJECTS.first(), EditorTool.AI_LAB)
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(NeuralCyan.copy(alpha = 0.14f))
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = title,
                                                    tint = NeuralCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = title,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = subtitle,
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recent Non-Destructive RAW Sessions (2x2 Grid)
                        item {
                            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent RAW Sessions",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Tap to Edit →",
                                        color = AmberBright,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                SAMPLE_PROJECTS.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { sample ->
                                            RawProjectCard(
                                                sample = sample,
                                                dummyState = previewDummyState,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onOpenSampleProject(sample, EditorTool.LIGHT) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HomeNavTab.PROJECTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Non-Destructive Cloud & Local Drafts",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Every slider, tone curve, and mask step is preserved.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        items(SAMPLE_PROJECTS) { sample ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StudioElevated)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                                    .clickable { onOpenSampleProject(sample, EditorTool.CURVES) }
                                    .padding(12.dp)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    drawProceduralRawScene(sample.sceneIndex, graded = true, state = previewDummyState)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sample.title,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = sample.lensExif,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${sample.editedAgo} · ${sample.steps} Edit Steps",
                                        color = AmberBright,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                HomeNavTab.LUT_LAB -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "3D Cinema LUTs & Analog Film Stocks",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Tap any film emulation LUT to launch directly in Pro Darkroom.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        items(STUDIO_LUTS.drop(1)) { lut ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StudioElevated)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                                    .clickable { onOpenWithLut(lut) }
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(lut.accent.copy(alpha = 0.2f))
                                            .border(1.dp, lut.accent, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lut.category.take(3).uppercase(),
                                            color = lut.accent,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = lut.name,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${lut.category} · 33-Point .CUBE LUT",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Text(
                                    text = "APPLY →",
                                    color = AmberBright,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                HomeNavTab.ENGINE -> {
                    EngineSettingsPanel()
                }
            }
        }

        // Bottom Native Darkroom Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface)
                .border(width = 1.dp, color = StudioBorder)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                HomeNavTab.STUDIO to Icons.Rounded.Home,
                HomeNavTab.PROJECTS to Icons.Rounded.FolderSpecial,
                HomeNavTab.LUT_LAB to Icons.Rounded.PhotoFilter,
                HomeNavTab.ENGINE to Icons.Rounded.Memory
            )
            navItems.forEach { (tab, icon) ->
                val selected = tab == activeTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { activeTab = tab }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label,
                        tint = if (selected) AmberBright else TextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = if (selected) AmberBright else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStudioTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StudioCard)
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RawProjectCard(
    sample: RawProjectSample,
    dummyState: EditorState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StudioElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawProceduralRawScene(sample.sceneIndex, graded = true, state = dummyState)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xCC090A0D))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = sample.formatBadge,
                    color = AmberBright,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xCC090A0D))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${sample.steps} steps",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = sample.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sample.editedAgo,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EngineSettingsPanel() {
    var gpuVulkan by remember { mutableStateOf(true) }
    var wideGamutP3 by remember { mutableStateOf(true) }
    var hapticTicks by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Native Android RAW Engine",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                text = "Hardware color pipeline & export configuration",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        item {
            SettingToggleCard(
                title = "Vulkan / Hardware GPU Acceleration",
                subtitle = "Zero-latency real-time 16-bit LUT & Tone Curve rendering",
                checked = gpuVulkan,
                onCheckedChange = { gpuVulkan = it }
            )
        }
        item {
            SettingToggleCard(
                title = "Display P3 Wide Color Gamut",
                subtitle = "Preserve deep cinema reds & cyan highlights on AMOLED panels",
                checked = wideGamutP3,
                onCheckedChange = { wideGamutP3 = it }
            )
        }
        item {
            SettingToggleCard(
                title = "Hardware Dial Haptic Ticks",
                subtitle = "Tactile micro-vibrations when scrubbing exposure & straighten dials",
                checked = hapticTicks,
                onCheckedChange = { hapticTicks = it }
            )
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkroomBg,
                checkedTrackColor = AmberGold
            )
        )
    }
}
