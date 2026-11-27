package com.lumacraft.studio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lumacraft.studio.vm.StudioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CameraScreen(vm: StudioViewModel) {
    val st by vm.ui.collectAsState()
    BackHandler { vm.goScreen(com.lumacraft.studio.model.Screen.HOME) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPerm = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPerm) permLauncher.launch(Manifest.permission.CAMERA)
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var showGrid by remember { mutableStateOf(true) }
    var timerSec by remember { mutableStateOf(0) }   // 0 / 3 / 10
    var countdown by remember { mutableStateOf(0) }
    var aspect by remember { mutableStateOf("Original") }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var busyCapturing by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // (re)bind camera when lens changes / permission granted
    LaunchedEffect(lensFacing, hasPerm) {
        if (!hasPerm) return@LaunchedEffect
        try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            provider.unbindAll()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            vm.notify("Camera unavailable: ${e.message ?: "error"}")
        }
    }

    fun doCapture() {
        if (busyCapturing) return
        busyCapturing = true
        imageCapture.flashMode = flashMode
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    val rotation = image.imageInfo.rotationDegrees
                    image.close()
                    busyCapturing = false
                    val oriented = if (rotation != 0) {
                        val m = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                        android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                    } else bitmap
                    vm.openCapturedPhoto(oriented, aspect)
                }

                override fun onError(exception: ImageCaptureException) {
                    busyCapturing = false
                    vm.notify("Capture failed: ${exception.message ?: "error"}")
                }
            }
        )
    }

    fun startCapture() {
        if (timerSec == 0) {
            doCapture()
        } else {
            scope.launch {
                for (i in timerSec downTo 1) {
                    countdown = i
                    delay(1000)
                }
                countdown = 0
                doCapture()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPerm) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            // rule-of-thirds grid
            if (showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val line = Color.White.copy(alpha = 0.45f)
                    for (i in 1..2) {
                        drawLine(line, androidx.compose.ui.geometry.Offset(size.width * i / 3f, 0f), androidx.compose.ui.geometry.Offset(size.width * i / 3f, size.height), 2f)
                        drawLine(line, androidx.compose.ui.geometry.Offset(0f, size.height * i / 3f), androidx.compose.ui.geometry.Offset(size.width, size.height * i / 3f), 2f)
                    }
                }
            }
            if (countdown > 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "${countdown}",
                        color = Color.White,
                        fontSize = 120.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission needed to capture photos", color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant camera access")
                    }
                }
            }
        }

        // ---- top control bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = { vm.goScreen(com.lumacraft.studio.model.Screen.HOME) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showGrid = !showGrid }) {
                Icon(Icons.Rounded.GridOn, "Grid", tint = if (showGrid) Color(0xFF8FB4FF) else Color.White)
            }
            IconButton(onClick = { timerSec = when (timerSec) { 0 -> 3; 3 -> 10; else -> 0 } }) {
                Icon(Icons.Rounded.Timer, "Timer", tint = if (timerSec > 0) Color(0xFF8FB4FF) else Color.White)
            }
            Text(
                if (timerSec > 0) "${timerSec}s" else "off",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.width(28.dp)
            )
            IconButton(onClick = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            }) {
                Icon(
                    when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                        else -> Icons.Rounded.FlashOff
                    },
                    "Flash",
                    tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) Color(0xFF8FB4FF) else Color.White
                )
            }
            IconButton(onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }) {
                Icon(Icons.Rounded.Cameraswitch, "Switch camera", tint = Color.White)
            }
        }

        // ---- bottom controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(vertical = 10.dp)
        ) {
            // exposure compensation
            if (hasPerm && camera != null) {
                val es = camera!!.cameraInfo.exposureState
                val range = es.exposureCompensationRange
                if (range.upper > range.lower) {
                    var expIdx by remember(es) { mutableStateOf(es.exposureCompensationIndex.toFloat()) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 28.dp)) {
                        Text("EV", color = Color.White, fontSize = 11.sp)
                        Slider(
                            value = expIdx.coerceIn(range.lower.toFloat(), range.upper.toFloat()),
                            onValueChange = {
                                expIdx = it
                                camera?.cameraControl?.setExposureCompensation(it.roundToInt())
                            },
                            valueRange = range.lower.toFloat()..range.upper.toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(30.dp)
                        )
                        Text("${es.exposureCompensationIndex}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(22.dp))
                    }
                }
            }
            // aspect chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                listOf("Original", "1:1", "4:5", "16:9", "9:16").forEach { a ->
                    val sel = aspect == a
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) Color(0xFF5B7CFA) else Color.White.copy(alpha = 0.15f))
                            .clickable { aspect = a }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(a, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { vm.goScreen(com.lumacraft.studio.model.Screen.HOME) }
                ) {
                    Text("Edit", color = Color.White, fontSize = 11.sp)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { if (hasPerm) startCapture() }
                ) {
                    if (busyCapturing) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(44.dp))
            }
            if (timerSec > 0) {
                Text("Timer ${timerSec}s · tap shutter to start countdown", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}
