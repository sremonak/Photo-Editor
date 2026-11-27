package com.lumacraft.studio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lumacraft.studio.ui.screens.BatchScreen
import com.lumacraft.studio.ui.screens.CollageScreen
import com.lumacraft.studio.ui.screens.EditorScreen
import com.lumacraft.studio.ui.screens.HomeScreen
import com.lumacraft.studio.ui.screens.PresetsScreen
import com.lumacraft.studio.ui.screens.SettingsScreen
import com.lumacraft.studio.ui.theme.LumaTheme
import com.lumacraft.studio.vm.StudioViewModel
import com.lumacraft.studio.model.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val st by vm.ui.collectAsState()
            val dark = when (st.settings.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }
            LumaTheme(dark = dark) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (st.screen) {
                            Screen.HOME -> HomeScreen(vm)
                            Screen.EDITOR -> EditorScreen(vm)
                            Screen.COLLAGE -> CollageScreen(vm)
                            Screen.BATCH -> BatchScreen(vm)
                            Screen.PRESETS -> PresetsScreen(vm)
                            Screen.SETTINGS -> SettingsScreen(vm)
                        }

                        val snackbarHostState = remember { SnackbarHostState() }
                        LaunchedEffect(st.toast) {
                            st.toast?.let {
                                snackbarHostState.showSnackbar(it)
                                vm.consumeToast()
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                        ) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = Color(0xFF1E2530),
                                contentColor = Color(0xFFEAF0FF)
                            )
                        }
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            Intent.ACTION_EDIT -> intent.data
            else -> null
        }
        if (uri != null && intent.action != null) {
            lifecycleScope.launch {
                // Persist read permission for the shared/edit URI
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
                vm.openPhoto(uri, uri.lastPathSegment ?: "Shared Photo")
            }
        }
    }
}
