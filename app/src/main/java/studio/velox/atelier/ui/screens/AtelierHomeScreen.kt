package studio.velox.atelier.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraRoll
import androidx.compose.material.icons.rounded.CropOriginal
import androidx.compose.material.icons.rounded.FilterBAndW
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LensBlur
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.velox.atelier.engine.BitmapGradeEngine
import studio.velox.atelier.model.AtelierHomeSection
import studio.velox.atelier.model.CONTACT_ROLL_SAMPLES
import studio.velox.atelier.model.ContactRollItem
import studio.velox.atelier.model.DarkroomModule
import studio.velox.atelier.model.FILM_STOCK_RECIPES
import studio.velox.atelier.model.FilmStockRecipe
import studio.velox.atelier.ui.theme.ChalkWhite
import studio.velox.atelier.ui.theme.ChampagneBrass
import studio.velox.atelier.ui.theme.CineMint
import studio.velox.atelier.ui.theme.CobaltMask
import studio.velox.atelier.ui.theme.GraphiteIsland
import studio.velox.atelier.ui.theme.KnurledCapsule
import studio.velox.atelier.ui.theme.LeicaVermilion
import studio.velox.atelier.ui.theme.MachinedBevel
import studio.velox.atelier.ui.theme.ObsidianStage
import studio.velox.atelier.ui.theme.PewterMuted
import studio.velox.atelier.ui.theme.SilverLabel
import studio.velox.atelier.ui.theme.TitaniumChassis
import studio.velox.atelier.ui.theme.VermilionSoft

@Composable
fun AtelierHomeScreen(
    onPickPhoto: () -> Unit,
    onOpenRollSample: (ContactRollItem, DarkroomModule) -> Unit,
    onOpenWithRecipe: (FilmStockRecipe) -> Unit
) {
    var activeSection by remember { mutableStateOf(AtelierHomeSection.ATELIER) }
    var cullingFilter by remember { mutableIntStateOf(0) }

    val sampleThumbs = remember {
        CONTACT_ROLL_SAMPLES.associate { item ->
            item.id to BitmapGradeEngine.generateStudioTestPlate(
                kind = item.sceneKind,
                width = 320,
                height = 400
            ).asImageBitmap()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianStage)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Industrial Camera-Shoulder Masthead
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Machined Lens Aperture Monolith Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GraphiteIsland)
                            .border(1.dp, ChampagneBrass.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(1.8.dp, ChampagneBrass, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(LeicaVermilion)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VELOX",
                                color = ChalkWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 2.2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(LeicaVermilion)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ATELIER 2.0",
                                    color = ChalkWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        Text(
                            text = "OPTICAL COLOR & FILM EMULSION LAB",
                            color = PewterMuted,
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
                        .background(GraphiteIsland)
                        .border(1.dp, MachinedBevel, CircleShape)
                        .clickable { activeSection = AtelierHomeSection.OPTICS_LAB }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Pipeline",
                        tint = ChampagneBrass,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Main Scrollable Atelier Content
            when (activeSection) {
                AtelierHomeSection.ATELIER -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Asymmetric Bento Intake + Direct Darkroom Modules
                        item {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF1A1C24),
                                                Color(0xFF101116)
                                            )
                                        )
                                    )
                                    .border(1.dp, MachinedBevel, RoundedCornerShape(26.dp))
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "NEGATIVE & RAW INTAKE",
                                            color = ChampagneBrass,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Develop Master Frame",
                                            color = ChalkWhite,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 21.sp,
                                            letterSpacing = (-0.4).sp
                                        )
                                        Text(
                                            text = "Real pixel pipeline · 3-Way Wheels · Halation · EXIF Print",
                                            color = SilverLabel,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(KnurledCapsule)
                                            .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "DNG · ARW · JPG",
                                            color = SilverLabel,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tactile Vermilion Shutter Release CTA
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(LeicaVermilion, VermilionSoft)
                                            )
                                        )
                                        .clickable { onPickPhoto() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddAPhoto,
                                        contentDescription = null,
                                        tint = ChalkWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Text(
                                        text = "Open Gallery / Import RAW",
                                        color = ChalkWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // 2x2 Bento Precision Tool Shortcuts
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BentoModuleCard(
                                        icon = Icons.Rounded.Flare,
                                        code = "HAL",
                                        title = "800T Halation",
                                        subtitle = "Red-orange scatter",
                                        accent = LeicaVermilion,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onOpenRollSample(CONTACT_ROLL_SAMPLES[1], DarkroomModule.ANALOG)
                                        }
                                    )
                                    BentoModuleCard(
                                        icon = Icons.Rounded.Palette,
                                        code = "3WY",
                                        title = "Color Wheels",
                                        subtitle = "Shadow · Mid · High",
                                        accent = ChampagneBrass,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onOpenRollSample(CONTACT_ROLL_SAMPLES[0], DarkroomModule.WHEELS)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BentoModuleCard(
                                        icon = Icons.Rounded.LensBlur,
                                        code = "MSK",
                                        title = "Selective Mask",
                                        subtitle = "Ruby overlay isolation",
                                        accent = CobaltMask,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onOpenRollSample(CONTACT_ROLL_SAMPLES[0], DarkroomModule.MASK)
                                        }
                                    )
                                    BentoModuleCard(
                                        icon = Icons.Rounded.CropOriginal,
                                        code = "PRT",
                                        title = "Gallery Rebate",
                                        subtitle = "Museum & EXIF stamp",
                                        accent = CineMint,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            onOpenRollSample(CONTACT_ROLL_SAMPLES[2], DarkroomModule.PRINT)
                                        }
                                    )
                                }
                            }
                        }

                        // Analog Film Emulsion Carousel
                        item {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Analog Emulsion Profiles",
                                        color = ChalkWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "DEHANCER-CLASS LUT",
                                        color = ChampagneBrass,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(FILM_STOCK_RECIPES.drop(1)) { stock ->
                                        Column(
                                            modifier = Modifier
                                                .width(138.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(GraphiteIsland)
                                                .border(1.dp, MachinedBevel, RoundedCornerShape(18.dp))
                                                .clickable { onOpenWithRecipe(stock) }
                                                .padding(13.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(stock.accent.copy(alpha = 0.16f))
                                                        .border(1.dp, stock.accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stock.code,
                                                        color = stock.accent,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Rounded.Grain,
                                                    contentDescription = null,
                                                    tint = PewterMuted,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = stock.name,
                                                color = ChalkWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${stock.category} · Halation ${stock.halation.toInt()}%",
                                                color = PewterMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 35mm Light-Table Contact Sheet Culling Grid
                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "35mm Contact Sheet",
                                        color = ChalkWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("All", "5★ Pick").forEachIndexed { idx, label ->
                                            val sel = cullingFilter == idx
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (sel) KnurledCapsule else Color.Transparent)
                                                    .border(1.dp, if (sel) ChampagneBrass else MachinedBevel, RoundedCornerShape(50))
                                                    .clickable { cullingFilter = idx }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (sel) ChampagneBrass else SilverLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val filteredRolls = if (cullingFilter == 1) {
                                    CONTACT_ROLL_SAMPLES.filter { it.ratingStars >= 5 }
                                } else {
                                    CONTACT_ROLL_SAMPLES
                                }

                                filteredRolls.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        pair.forEach { roll ->
                                            ContactFrameCard(
                                                roll = roll,
                                                thumb = sampleThumbs[roll.id],
                                                modifier = Modifier.weight(1f),
                                                onClick = { onOpenRollSample(roll, DarkroomModule.LIGHT) }
                                            )
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AtelierHomeSection.CONTACT_SHEET -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Light Table & Flagged Selects",
                                color = ChalkWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Non-destructive RAW + JPEG paired rolls ready for grading.",
                                color = SilverLabel,
                                fontSize = 12.sp
                            )
                        }
                        items(CONTACT_ROLL_SAMPLES) { roll ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(GraphiteIsland)
                                    .border(1.dp, MachinedBevel, RoundedCornerShape(18.dp))
                                    .clickable { onOpenRollSample(roll, DarkroomModule.CURVES) }
                                    .padding(12.dp)
                            ) {
                                sampleThumbs[roll.id]?.let { bmp ->
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = roll.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "▶ ${roll.frameCode}",
                                            color = ChampagneBrass,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(roll.flagColor)
                                        )
                                    }
                                    Text(
                                        text = roll.title,
                                        color = ChalkWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = roll.exif.exposureSummary,
                                        color = PewterMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                AtelierHomeSection.RECIPES -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Master Film Chemistry Vault",
                                color = ChalkWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Calibrated halation, tone shoulder, and split-wheel stocks.",
                                color = SilverLabel,
                                fontSize = 12.sp
                            )
                        }
                        items(FILM_STOCK_RECIPES.drop(1)) { stock ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(GraphiteIsland)
                                    .border(1.dp, MachinedBevel, RoundedCornerShape(18.dp))
                                    .clickable { onOpenWithRecipe(stock) }
                                    .padding(15.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "${stock.code} · ${stock.name}",
                                        color = ChalkWhite,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${stock.category}  |  Halation ${stock.halation.toInt()}% · Grain ${stock.grain.toInt()}%",
                                        color = SilverLabel,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(LeicaVermilion.copy(alpha = 0.16f))
                                        .border(1.dp, LeicaVermilion, RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "LOAD",
                                        color = LeicaVermilion,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                AtelierHomeSection.OPTICS_LAB -> {
                    OpticsPipelineSettings()
                }
            }
        }

        // 3. Floating Material 3 Expressive Morphing Pill Navigation Bar!
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(TitaniumChassis.copy(alpha = 0.96f))
                    .border(1.dp, MachinedBevel, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    AtelierHomeSection.ATELIER to Icons.Rounded.AutoAwesome,
                    AtelierHomeSection.CONTACT_SHEET to Icons.Rounded.CameraRoll,
                    AtelierHomeSection.RECIPES to Icons.Rounded.FilterBAndW,
                    AtelierHomeSection.OPTICS_LAB to Icons.Rounded.Memory
                )
                tabs.forEach { (sec, icon) ->
                    val active = activeSection == sec
                    val pillColor by animateColorAsState(
                        targetValue = if (active) LeicaVermilion else Color.Transparent,
                        label = "NavPillColor"
                    )
                    val hPad by animateDpAsState(
                        targetValue = if (active) 16.dp else 12.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "NavPillWidth"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(pillColor)
                            .clickable { activeSection = sec }
                            .padding(horizontal = hPad, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = sec.label,
                            tint = if (active) ChalkWhite else SilverLabel,
                            modifier = Modifier.size(18.dp)
                        )
                        if (active) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sec.label,
                                color = ChalkWhite,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoModuleCard(
    icon: ImageVector,
    code: String,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KnurledCapsule.copy(alpha = 0.72f))
            .border(1.dp, MachinedBevel, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = code,
                color = PewterMuted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = ChalkWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(
            text = subtitle,
            color = PewterMuted,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ContactFrameCard(
    roll: ContactRollItem,
    thumb: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(GraphiteIsland)
            .border(1.dp, MachinedBevel, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        // Top 35mm Negative Sprocket Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▶ ${roll.frameCode}",
                color = ChampagneBrass,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(roll.ratingStars) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = ChampagneBrass,
                        modifier = Modifier.size(9.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(roll.flagColor)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = roll.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xD908080A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = roll.stockBadge,
                    color = ChalkWhite,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = roll.title,
            color = ChalkWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = roll.exif.cameraModel,
            color = PewterMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun OpticsPipelineSettings() {
    var realHalation by remember { mutableStateOf(true) }
    var exifAutoRead by remember { mutableStateOf(true) }
    var rotaryHaptics by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "VELOX Color & Optical Engine",
                color = ChalkWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = "Multi-threaded Kotlin pixel pipeline & MediaStore RAW/RGB output",
                color = SilverLabel,
                fontSize = 12.sp
            )
        }
        item {
            PipelineSwitchTile(
                title = "Separable Box-Blur Emulsion Halation",
                subtitle = "Simulate authentic red-orange silver film scatter on bright highlights",
                checked = realHalation,
                onChange = { realHalation = it }
            )
        }
        item {
            PipelineSwitchTile(
                title = "Automatic EXIF Lens & Shutter Extraction",
                subtitle = "Reads real ISO, f-stop, focal length & camera body on gallery import",
                checked = exifAutoRead,
                onChange = { exifAutoRead = it }
            )
        }
        item {
            PipelineSwitchTile(
                title = "Cylindrical Lens-Ring Detent Haptics",
                subtitle = "Tactile micro-detents while rotating exposure & geometry barrels",
                checked = rotaryHaptics,
                onChange = { rotaryHaptics = it }
            )
        }
    }
}

@Composable
private fun PipelineSwitchTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GraphiteIsland)
            .border(1.dp, MachinedBevel, RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = ChalkWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, color = SilverLabel, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ChalkWhite,
                checkedTrackColor = LeicaVermilion
            )
        )
    }
}
