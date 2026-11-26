package com.luminapro.photoeditor

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luminapro.photoeditor.model.AppScreen
import com.luminapro.photoeditor.model.EditorState
import com.luminapro.photoeditor.model.EditorTool
import com.luminapro.photoeditor.ui.screens.ProEditorScreen
import com.luminapro.photoeditor.ui.screens.StudioHomeScreen
import com.luminapro.photoeditor.ui.theme.LuminaPhotoEditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#090A0D")),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#090A0D"))
        )

        setContent {
            LuminaPhotoEditorTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                val historyStack = remember { mutableStateListOf(EditorState()) }
                var historyIndex by remember { mutableIntStateOf(0) }

                val currentState = historyStack[historyIndex]

                fun pushEditorState(updated: EditorState) {
                    // Keep responsive scrubbing smooth while tracking meaningful state history
                    if (historyIndex < historyStack.lastIndex) {
                        historyStack.removeRange(historyIndex + 1, historyStack.size)
                    }
                    if (historyStack.size > 40) {
                        historyStack.removeAt(0)
                    } else {
                        historyIndex++
                    }
                    historyStack.add(updated)
                    historyIndex = historyStack.lastIndex
                }

                val pickPhotoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        val fresh = EditorState(
                            pickedImageUri = uri,
                            activeTool = EditorTool.LIGHT
                        )
                        historyStack.clear()
                        historyStack.add(fresh)
                        historyIndex = 0
                        currentScreen = AppScreen.EDITOR
                    }
                }

                BackHandler(enabled = currentScreen == AppScreen.EDITOR) {
                    currentScreen = AppScreen.HOME
                }

                Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        AppScreen.HOME -> {
                            StudioHomeScreen(
                                onPickGalleryPhoto = {
                                    pickPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onOpenSampleProject = { sample, initialTool ->
                                    val fresh = EditorState(
                                        activeSample = sample,
                                        pickedImageUri = null,
                                        activeTool = initialTool
                                    )
                                    historyStack.clear()
                                    historyStack.add(fresh)
                                    historyIndex = 0
                                    currentScreen = AppScreen.EDITOR
                                },
                                onOpenWithLut = { lut ->
                                    val fresh = EditorState(
                                        activeTool = EditorTool.LUTS,
                                        activePresetId = lut.id,
                                        exposure = lut.exposure,
                                        contrast = lut.contrast,
                                        temperature = lut.warmth,
                                        saturation = lut.saturation,
                                        vignette = lut.vignette,
                                        fade = lut.fade
                                    )
                                    historyStack.clear()
                                    historyStack.add(fresh)
                                    historyIndex = 0
                                    currentScreen = AppScreen.EDITOR
                                }
                            )
                        }

                        AppScreen.EDITOR -> {
                            ProEditorScreen(
                                state = currentState,
                                canUndo = historyIndex > 0,
                                canRedo = historyIndex < historyStack.lastIndex,
                                historyCount = historyIndex + 1,
                                onUpdateState = { next -> pushEditorState(next) },
                                onUndo = {
                                    if (historyIndex > 0) historyIndex--
                                },
                                onRedo = {
                                    if (historyIndex < historyStack.lastIndex) historyIndex++
                                },
                                onBack = { currentScreen = AppScreen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }
}
