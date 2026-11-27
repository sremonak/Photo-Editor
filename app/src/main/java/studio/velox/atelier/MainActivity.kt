package studio.velox.atelier

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.velox.atelier.engine.BitmapGradeEngine
import studio.velox.atelier.model.AtelierScreen
import studio.velox.atelier.model.CurveChannelId
import studio.velox.atelier.model.DarkroomModule
import studio.velox.atelier.model.DarkroomState
import studio.velox.atelier.ui.screens.AtelierHomeScreen
import studio.velox.atelier.ui.screens.DarkroomEditorScreen
import studio.velox.atelier.ui.theme.VeloxAtelierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#08080A")),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#08080A"))
        )

        // Support opening images directly from Android's Share / Edit With menu!
        val incomingUri: Uri? = when (intent?.action) {
            Intent.ACTION_EDIT -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }

        setContent {
            VeloxAtelierTheme {
                val context = LocalContext.current
                var screen by remember {
                    mutableStateOf(
                        if (incomingUri != null) AtelierScreen.DARKROOM else AtelierScreen.ATELIER_HOME
                    )
                }

                val initialState = remember {
                    DarkroomState(pickedUri = incomingUri)
                }
                val history = remember { mutableStateListOf(initialState) }
                var historyIdx by remember { mutableIntStateOf(0) }
                val activeState = history[historyIdx]

                var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }

                // Decode source bitmap + real camera EXIF whenever pickedUri or sample roll changes
                LaunchedEffect(activeState.pickedUri, activeState.activeRoll.id) {
                    val loaded = withContext(Dispatchers.IO) {
                        BitmapGradeEngine.loadUriOrSample(
                            context = context,
                            uri = activeState.pickedUri,
                            sampleSceneKind = activeState.activeRoll.sceneKind,
                            fallbackExif = activeState.activeRoll.exif,
                            maxDimension = 1280
                        )
                    }
                    sourceBitmap = loaded.bitmap
                    if (activeState.pickedUri != null && activeState.exif != loaded.exif) {
                        history[historyIdx] = activeState.copy(exif = loaded.exif)
                    }
                }

                fun commitState(next: DarkroomState) {
                    if (historyIdx < history.lastIndex) {
                        history.removeRange(historyIdx + 1, history.size)
                    }
                    if (history.size > 45) {
                        history.removeAt(0)
                    } else {
                        historyIdx++
                    }
                    history.add(next)
                    historyIdx = history.lastIndex
                }

                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        val fresh = DarkroomState(
                            pickedUri = uri,
                            activeModule = DarkroomModule.LIGHT
                        )
                        history.clear()
                        history.add(fresh)
                        historyIdx = 0
                        screen = AtelierScreen.DARKROOM
                    }
                }

                BackHandler(enabled = screen == AtelierScreen.DARKROOM) {
                    screen = AtelierScreen.ATELIER_HOME
                }

                Crossfade(targetState = screen, label = "AtelierNavigation") { target ->
                    when (target) {
                        AtelierScreen.ATELIER_HOME -> {
                            AtelierHomeScreen(
                                onPickPhoto = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onOpenRollSample = { roll, module ->
                                    val fresh = DarkroomState(
                                        activeRoll = roll,
                                        pickedUri = null,
                                        exif = roll.exif,
                                        activeModule = module
                                    )
                                    history.clear()
                                    history.add(fresh)
                                    historyIdx = 0
                                    screen = AtelierScreen.DARKROOM
                                },
                                onOpenWithRecipe = { stock ->
                                    val fresh = DarkroomState(
                                        activeModule = DarkroomModule.FILM,
                                        activeRecipeId = stock.id,
                                        exposure = stock.exposure,
                                        contrast = stock.contrast,
                                        highlights = stock.highlights,
                                        shadows = stock.shadows,
                                        temperature = stock.temperature,
                                        vibrance = stock.vibrance,
                                        saturation = stock.saturation,
                                        halation = stock.halation,
                                        bloom = stock.bloom,
                                        grain = stock.grain,
                                        matteFade = stock.matteFade,
                                        shadowWheel = stock.shadowWheel,
                                        highlightWheel = stock.highlightWheel,
                                        curves = mapOf(
                                            CurveChannelId.LUMA to stock.lumaCurve,
                                            CurveChannelId.RED to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                                            CurveChannelId.GREEN to listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                                            CurveChannelId.BLUE to listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                                        )
                                    )
                                    history.clear()
                                    history.add(fresh)
                                    historyIdx = 0
                                    screen = AtelierScreen.DARKROOM
                                }
                            )
                        }

                        AtelierScreen.DARKROOM -> {
                            DarkroomEditorScreen(
                                state = activeState,
                                sourceBitmap = sourceBitmap,
                                canUndo = historyIdx > 0,
                                canRedo = historyIdx < history.lastIndex,
                                stepNumber = historyIdx + 1,
                                onUpdateState = { commitState(it) },
                                onUndo = { if (historyIdx > 0) historyIdx-- },
                                onRedo = { if (historyIdx < history.lastIndex) historyIdx++ },
                                onBack = { screen = AtelierScreen.ATELIER_HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
